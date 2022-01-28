/*
 * Copyright 2021-2022 Exactpro (Exactpro Systems Limited)
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
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.toJson
import com.exactpro.th2.codec.xml.utils.toJson
import com.exactpro.th2.codec.xml.utils.toProto
import com.exactpro.th2.codec.xml.xsd.XsdValidator
import com.exactpro.th2.common.message.messageType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.underscore.lodash.Json
import com.github.underscore.lodash.U
import com.google.protobuf.ByteString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.nio.file.Path


open class XmlPipelineCodec(private val settings: XmlPipelineCodecSettings, xsdMap: Map<String, Path>)  : IPipelineCodec {

    private var xmlCharset: Charset = Charsets.UTF_8
    private val validator = XsdValidator(xsdMap, settings.dirtyValidation)

    override fun encode(messageGroup: MessageGroup): MessageGroup {
        val messages = messageGroup.messagesList
        if (messages.none { it.hasMessage() }) {
            return messageGroup
        }

        return MessageGroup.newBuilder().addAllMessages(
            messages.map { anyMsg ->
                if (anyMsg.hasMessage() && checkProtocol(anyMsg.message.metadata.protocol))
                    AnyMessage.newBuilder().setRawMessage(encodeOne(anyMsg.message)).build()
                else anyMsg
            }
        ).build()
    }

    //FIXME: move this check into the codec-core project
    private fun checkProtocol(msgProtocol: String?): Boolean {
        return msgProtocol.isNullOrEmpty() || msgProtocol == XmlPipelineCodecFactory.PROTOCOL
    }

    private fun encodeOne(message: Message): RawMessage {

        val json = message.toJson()

        val xmlString = U.jsonToXml(json)

        validator.validate(xmlString.toByteArray())
        LOGGER.info("Validation of incoming parsed message complete: ${message.messageType}")

        return RawMessage.newBuilder().apply {
            parentEventId = message.parentEventId
            metadataBuilder.putAllProperties(message.metadata.propertiesMap)
            metadataBuilder.protocol = XmlPipelineCodecFactory.PROTOCOL
            metadataBuilder.id = message.metadata.id
            metadataBuilder.timestamp = message.metadata.timestamp
            body = ByteString.copyFrom(xmlString, xmlCharset)
        }.build()
    }


    override fun decode(messageGroup: MessageGroup): MessageGroup {
        val messages = messageGroup.messagesList
        if (messages.none { it.hasRawMessage() }) {
            return messageGroup
        }

        return MessageGroup.newBuilder().apply {
            messages.forEach { input ->
                if (input.hasRawMessage() && checkProtocol(input.rawMessage.metadata.protocol))
                    try {
                        addMessages(AnyMessage.newBuilder().setMessage(decodeOne(input.rawMessage)).build())
                    } catch (e: Exception) {
                        throw IllegalStateException("Can not decode message = ${input.rawMessage.toJson()}", e)
                    }
                else {
                    addMessages(input)
                }
            }
        }.build()
    }

    private fun decodeOne(rawMessage: RawMessage): Message {
        try {
            validator.validate(rawMessage.body.toByteArray())
            LOGGER.info("Validation of incoming raw message complete: ${rawMessage.metadata.idOrBuilder}")
            val xmlString = rawMessage.body.toStringUtf8()
            val jsonString = U.xmlToJson(xmlString, Json.JsonStringBuilder.Step.COMPACT, null )

            val jsonNode: JsonNode = jsonMapper.readTree(jsonString)

            check(jsonNode.size()==1) {"There was more than one root node in processed xml, result json have ${jsonNode.size()}"}

            val msgType: String = settings.typePointer?.let {
                val typeNode = jsonNode.at(it)
                typeNode.asText()
            } ?: jsonNode.fieldNames().next()

            check(jsonNode.size()==1) {"There more then one root messages after xml to Node process"}

            return jsonNode.toProto(msgType, rawMessage)
        } catch (e: Exception) {
            throw DecodeException("Can not decode message. Can not parse XML. ${rawMessage.body.toStringUtf8()}", e)
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(XmlPipelineCodec::class.java)
        private val jsonMapper = JsonMapper()
    }
}