/*
 * Copyright 2021-2023 Exactpro (Exactpro Systems Limited)
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
import com.github.underscore.Xml
import com.google.protobuf.ByteString
import io.netty.buffer.Unpooled
import mu.KotlinLogging
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.Locale

open class XmlPipelineCodec(private val settings: XmlPipelineCodecSettings, xsdMap: Map<String, Path>)  : IPipelineCodec {

    private val pointer = settings.typePointer?.split("/")?.filterNot { it.isBlank() }
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
        return msgProtocol.isNullOrEmpty() || msgProtocol == XmlPipelineCodecFactory.PROTOCOL
    }

    private fun encodeOne(message: ProtoMessage): ProtoRawMessage {
        val map = message.toMap()
        val xmlString = Xml.toXml(map)

        validator.validate(xmlString.toByteArray())
        LOGGER.debug { "Validation of incoming parsed message complete: ${message.messageType}" }

        return ProtoRawMessage.newBuilder().apply {
            if (message.hasParentEventId()) parentEventId = message.parentEventId
            metadataBuilder.putAllProperties(message.metadata.propertiesMap)
            metadataBuilder.protocol = XmlPipelineCodecFactory.PROTOCOL
            metadataBuilder.id = message.metadata.id
            body = ByteString.copyFrom(xmlString, xmlCharset)
        }.build()
    }

    private fun encodeOne(message: ParsedMessage): RawMessage {
        val map = message.body
        val xmlString = Xml.toXml(map)

        validator.validate(xmlString.toByteArray())
        LOGGER.debug { "Validation of incoming parsed message complete: ${message.type}" }

        return RawMessage(
            id = message.id,
            eventId = message.eventId,
            metadata = message.metadata,
            protocol = XmlPipelineCodecFactory.PROTOCOL,
            body = Unpooled.copiedBuffer(xmlString, xmlCharset)
        )
    }

    override fun decode(messageGroup: ProtoMessageGroup, context: IReportingContext): ProtoMessageGroup {
        val messages = messageGroup.messagesList
        if (messages.none { it.hasRawMessage() }) {
            return messageGroup
        }

        return ProtoMessageGroup.newBuilder().apply {
            messages.forEach { input ->
                if (input.hasRawMessage() && checkProtocol(input.rawMessage.metadata.protocol))
                    try {
                        addMessages(ProtoAnyMessage.newBuilder().setMessage(decodeOneProto(input.rawMessage)).build())
                    } catch (e: Exception) {
                        throw IllegalStateException("Can not decode message = ${input.rawMessage.toJson()}", e)
                    }
                else {
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
                if (input is RawMessage && checkProtocol(input.protocol))
                    try {
                        decodeOneTransport(input)
                    } catch (e: Exception) {
                        throw IllegalStateException("Can not decode message = $input", e)
                    }
                else {
                    input
                }
            }
        )
    }

    private fun decodeOne(body: ByteArray, xmlString: String, logId: String): Pair<String, MutableMap<String, *>> {
        try {
            validator.validate(body)
            LOGGER.debug { "Validation of incoming raw message complete: $logId" }
            @Suppress("UNCHECKED_CAST")
            val map = Xml.fromXml(xmlString) as MutableMap<String, *>

            LOGGER.trace { "Result of the 'Xml.fromXml' method is ${map.keys} for $xmlString" }
            map -= STANDALONE
            map -= ENCODING

            if (OMIT_XML_DECLARATION in map) {
                // U library will tell by this option is there no declaration
                check(!settings.expectsDeclaration || map[OMIT_XML_DECLARATION] == NO) { "Expecting declaration inside xml data" }
                map -= OMIT_XML_DECLARATION
            }

            if (map.size > 1) {
                error("There was more than one root node in processed xml, result json has [${map.size}]: ${map.keys.joinToString(", ")}")
            }

            val msgType: String = pointer?.let { map.getNode<String>(it) } ?: map.keys.first()

            return msgType to map
        } catch (e: Exception) {
            throw DecodeException("Can not decode message. Can not parse XML. $xmlString", e)
        }
    }

    private fun decodeOneProto(rawMessage: ProtoRawMessage): ProtoMessage {
        val xmlString = rawMessage.body.toStringUtf8()

        try {
            val (msgType, map) = decodeOne(rawMessage.body.toByteArray(), xmlString, rawMessage.logId)
            return map.toProto(msgType, rawMessage)
        } catch (e: Exception) {
            throw DecodeException("Can not decode message. Can not parse XML. $xmlString", e)
        }
    }

    private val RawMessage.logId: String
        get() = "${id.sessionAlias}:${id.direction.toString().lowercase(Locale.getDefault())}:${id.sequence}${id.subsequence.joinToString("") { ".$it" }}"

    private fun decodeOneTransport(rawMessage: RawMessage): ParsedMessage {
        val xmlString = rawMessage.body.toString(Charsets.UTF_8)
        try {
            val (msgType, map) = decodeOne(rawMessage.body.toByteArray(), xmlString, rawMessage.logId)
            return map.toTransport(msgType, rawMessage)
        } catch (e: Exception) {
            throw DecodeException("Can not decode message. Can not parse XML. $xmlString", e)
        }
    }

    private inline fun <reified T>Map<*,*>.getNode(pointer: List<String>): T {
        var current: Any = this

        for (name in pointer) {
            current = (current as? Map<*, *>)?.get(name) ?: error("Can not find element by name '$name' in path: $pointer")
        }
        return current as T
    }

    companion object {
        private val LOGGER = KotlinLogging.logger {}

        private const val NO = "no"

        /**
         * The constant from [Xml.OMITXMLDECLARATION]
         */
        private const val OMIT_XML_DECLARATION = "#omit-xml-declaration"
        /**
         * The constant from [Xml.ENCODING]
         */
        private const val ENCODING = "#encoding"
        /**
         * The constant from [Xml.STANDALONE]
         */
        private const val STANDALONE = "#standalone"
    }
}