/*
 * Copyright 2021-2024 Exactpro (Exactpro Systems Limited)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.DecodeException
import com.exactpro.th2.codec.api.IPipelineCodec
import com.exactpro.th2.codec.api.IReportingContext
import com.exactpro.th2.codec.xml.utils.toMap
import com.exactpro.th2.codec.xml.utils.toProto
import com.exactpro.th2.codec.xml.utils.toTransport
import com.exactpro.th2.codec.xml.xsd.XsdValidator
import com.exactpro.th2.common.message.messageType
import com.exactpro.th2.common.message.toJson
import com.exactpro.th2.common.message.logId
import com.exactpro.th2.common.grpc.AnyMessage as ProtoAnyMessage
import com.exactpro.th2.common.grpc.Message as ProtoMessage
import com.exactpro.th2.common.grpc.MessageGroup as ProtoMessageGroup
import com.exactpro.th2.common.grpc.RawMessage as ProtoRawMessage
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageGroup
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.RawMessage
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.toByteArray
import com.exactpro.th2.common.utils.message.transport.getField
import com.github.underscore.Xml
import com.google.protobuf.UnsafeByteOperations
import io.netty.buffer.Unpooled
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream
import java.nio.charset.Charset
import java.util.Locale

open class XmlPipelineCodec(settings: XmlPipelineCodecSettings, xsdMap: Map<String, () -> InputStream> = emptyMap())  : IPipelineCodec {
    private val pointer: List<String> = settings.typePointer
        ?.split("/")?.filter(String::isNotBlank)
        ?: listOf()
    private var xmlCharset: Charset = Charsets.UTF_8
    private val validator = XsdValidator(xsdMap, settings.dirtyValidation)

    override fun encode(messageGroup: ProtoMessageGroup, context: IReportingContext): ProtoMessageGroup {
        val messages = messageGroup.messagesList
        if (messages.none { it.hasMessage() }) {
            return messageGroup
        }

        return ProtoMessageGroup.newBuilder().addAllMessages(
            messages.map { anyMsg ->
                if (anyMsg.hasMessage() && checkProtocol(anyMsg.message.metadata.protocol))
                    ProtoAnyMessage.newBuilder().setRawMessage(encodeOne(anyMsg.message)).build()
                else anyMsg
            }
        ).build()
    }

    override fun encode(messageGroup: MessageGroup, context: IReportingContext): MessageGroup {
        val messages = messageGroup.messages
        if (messages.none { it is ParsedMessage }) {
            return messageGroup
        }

        return MessageGroup(
            messages.map { anyMsg ->
                if (anyMsg is ParsedMessage && checkProtocol(anyMsg.protocol))
                    encodeOne(anyMsg)
                else anyMsg
            }
        )
    }

    //FIXME: move this check into the codec-core project
    private fun checkProtocol(msgProtocol: String?): Boolean {
        return msgProtocol.isNullOrEmpty() || XmlPipelineCodecFactory.PROTOCOL.equals(msgProtocol, true)
    }

    private fun encodeOne(message: ProtoMessage): ProtoRawMessage {
        val map = message.toMap()
        val xmlStringBuffer = Xml.toXml(map).toByteArray(xmlCharset)

        validator.validate(xmlStringBuffer)
        LOGGER.debug { "Validation of incoming parsed message complete: ${message.messageType}" }

        return ProtoRawMessage.newBuilder().apply {
            if (message.hasParentEventId()) parentEventId = message.parentEventId
            metadataBuilder.putAllProperties(message.metadata.propertiesMap)
            metadataBuilder.protocol = XmlPipelineCodecFactory.PROTOCOL
            metadataBuilder.id = message.metadata.id
            body = UnsafeByteOperations.unsafeWrap(xmlStringBuffer)
        }.build()
    }

    private fun encodeOne(message: ParsedMessage): RawMessage {
        val map = message.body
        val xmlStringBuffer = Xml.toXml(map).toByteArray(xmlCharset)

        validator.validate(xmlStringBuffer)
        LOGGER.debug { "Validation of incoming parsed message complete: ${message.type}" }

        return RawMessage(
            id = message.id,
            eventId = message.eventId,
            metadata = message.metadata,
            protocol = XmlPipelineCodecFactory.PROTOCOL,
            body = Unpooled.wrappedBuffer(xmlStringBuffer)
        )
    }

    override fun decode(messageGroup: ProtoMessageGroup, context: IReportingContext): ProtoMessageGroup {
        val messages = messageGroup.messagesList
        if (messages.none { it.hasRawMessage() }) {
            return messageGroup
        }

        return ProtoMessageGroup.newBuilder().apply {
            messages.forEach { input ->
                if (input.hasRawMessage() && checkProtocol(input.rawMessage.metadata.protocol)) {
                    try {
                        addMessages(ProtoAnyMessage.newBuilder().setMessage(decodeOneProto(input.rawMessage)).build())
                    } catch (e: Exception) {
                        throw IllegalStateException("Can not decode message = ${input.rawMessage.toJson()}", e)
                    }
                } else {
                    addMessages(input)
                }
            }
        }.build()
    }

    override fun decode(messageGroup: MessageGroup, context: IReportingContext): MessageGroup {
        val messages = messageGroup.messages
        if (messages.none { it is RawMessage }) {
            return messageGroup
        }

        return MessageGroup(
            messages.map { input ->
                if (input is RawMessage && checkProtocol(input.protocol)) {
                    try {
                        decodeOneTransport(input)
                    } catch (e: Exception) {
                        throw IllegalStateException("Can not decode message = $input", e)
                    }
                } else {
                    input
                }
            }
        )
    }

    private fun decodeOne(body: ByteArray, xmlString: String, logId: String): Pair<String, MutableMap<String, *>> {
        try {
            validator.validate(body)

            XmlCodecStreamReader(body, { linkedMapOf() }, TransportFieldAppender).use { reader ->
                while (reader.hasNext()) {
                    reader.next()
                }
                val message: MutableMap<String, Any> = reader.getMessage()
                if (message.size > 1) {
                    error(
                        "There was more than one root node in processed xml, result json has [${message.size}]: " +
                                message.keys.joinToString(", ")
                    )
                }
                val msgType: String = if (pointer.isEmpty()) {
                    message.keys.first() // first tag name
                } else {
                    checkNotNull(message.getField(*pointer.toTypedArray())?.toString()) {
                        "message type at $pointer is null in message $message"
                    }
                }
                return msgType to message
            }
        } catch (e: Exception) {
            throw DecodeException("Can not decode message ${logId}. Can not parse XML. $xmlString", e)
        }
    }

    private fun decodeOneProto(rawMessage: ProtoRawMessage): ProtoMessage {
        val xmlString = rawMessage.body.toString(Charsets.UTF_8)
        try {
            val (msgType, map) = decodeOne(rawMessage.body.toByteArray(), xmlString, rawMessage.logId)
            return map.toProto(msgType, rawMessage)
        } catch (e: Exception) {
            throw DecodeException("Can not decode message. Can not parse XML. $xmlString", e)
        }
    }


    private fun decodeOneTransport(rawMessage: RawMessage): ParsedMessage {
        val xmlString = rawMessage.body.toString(Charsets.UTF_8)
        try {
            val (msgType, map) = decodeOne(rawMessage.body.toByteArray(), xmlString, rawMessage.logId)
            return map.toTransport(msgType, rawMessage)
        } catch (e: Exception) {
            throw DecodeException("Can not decode message. Can not parse XML. $xmlString", e)
        }
    }

    private val RawMessage.logId: String
        get() = "${id.sessionAlias}:${id.direction.toString().lowercase(Locale.getDefault())}:${id.sequence}${id.subsequence.joinToString("") { ".$it" }}"

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }
}