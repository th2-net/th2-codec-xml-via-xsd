/*
 * Copyright 2021-2021 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.codec.CodecException
import com.exactpro.th2.codec.DecodeException
import com.exactpro.th2.codec.api.IPipelineCodec
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.getString
import com.exactpro.th2.common.message.messageType
import com.exactpro.th2.common.message.toJson
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.underscore.lodash.Json
import com.github.underscore.lodash.U
import com.google.protobuf.ByteString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xml.sax.SAXException
import java.io.IOException
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathFactory


open class XmlPipelineCodec(private val settings: XmlPipelineCodecSettings)  : IPipelineCodec {

    private var xmlCharset: Charset = Charsets.UTF_8

    override fun encode(messageGroup: MessageGroup): MessageGroup {
        val messages = messageGroup.messagesList
        if (messages.none { it.hasMessage() }) {
            return messageGroup
        }

        return MessageGroup.newBuilder().addAllMessages(
            messages.map { anyMsg ->
                if (anyMsg.hasMessage() && anyMsg.message.metadata.protocol.let { msgProtocol -> msgProtocol.isNullOrEmpty() || msgProtocol == XmlPipelineCodecFactory.PROTOCOL })
                    AnyMessage.newBuilder().setRawMessage(encodeOne(anyMsg.message)).build()
                else anyMsg
            }
        ).build()
    }

    private fun encodeOne(message: Message): RawMessage {

        val jsonField = checkNotNull(message.getString("json")) {"There no json inside encoding message: $message"}

        val xmlString = U.jsonToXml(jsonField)

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
                if (input.hasRawMessage())
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
            val messageBuilder = Message.newBuilder()
            val xmlString = rawMessage.body.toStringUtf8()

            var jsonString = U.xmlToJson(xmlString, Json.JsonStringBuilder.Step.COMPACT, null )

            val jsonNode: JsonNode = jsonMapper.readTree(jsonString)

            check(jsonNode.size()==1) {"There was more than one root node in processed xml, result json have ${jsonNode.size()}"}

            val msgType: String = settings.typePointer?.let {
                val typeNode = jsonNode.at(it)
                typeNode.asText()
            } ?: jsonNode.fieldNames().next()

            check(jsonNode.size()==1) {"There more then one root messages after xml to Node process"}

            return messageBuilder.apply {
                messageType = msgType
                parentEventId = rawMessage.parentEventId
                metadataBuilder.also { msgMetadata ->
                    rawMessage.metadata.also { rawMetadata ->
                        msgMetadata.id = rawMetadata.id
                        msgMetadata.timestamp = rawMetadata.timestamp
                        msgMetadata.protocol = XmlPipelineCodecFactory.PROTOCOL
                        msgMetadata.putAllProperties(rawMetadata.propertiesMap)
                    }
                }
                addField("json", jsonString)
            }.build()
        } catch (e: Exception) {
            when (e) {
                is IOException,
                is SAXException -> {
                    throw DecodeException("Can not decode message. Can not parse XML. ${rawMessage.toJson()}", e)
                }
                else -> throw e
            }
        }
    }

    private fun JsonNode.renameField(oldName: String, newName: String) : JsonNode {
        (this as ObjectNode).apply {
            set<JsonNode>(newName, get(oldName))
            remove(oldName)
        }
        return this
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(XmlPipelineCodec::class.java)

        private val jsonMapper = JsonMapper()

        private val X_PATH: ThreadLocal<XPath> = ThreadLocal.withInitial {
            XPathFactory.newInstance().newXPath()
        }

        private val DOCUMENT_BUILDER: ThreadLocal<DocumentBuilder> = ThreadLocal.withInitial {
            try {
                DocumentBuilderFactory.newInstance().newDocumentBuilder()
            } catch (e: ParserConfigurationException) {
                throw CodecException("Error while initialization. Can not create DocumentBuilderFactory", e)
            }
        }
    }
}