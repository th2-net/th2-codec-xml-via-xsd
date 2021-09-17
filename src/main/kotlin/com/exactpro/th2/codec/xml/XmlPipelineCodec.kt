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
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
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
import com.github.underscore.lodash.U



open class XmlPipelineCodec(private val settings: XmlPipelineCodecSettings?)  : IPipelineCodec {

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

//        val jsonMapper: ObjectMapper = JsonMapper()
//        val jsonNode: ObjectNode = jsonMapper.readTree(message.getString("json")) as ObjectNode
//        xmlMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
//
//        val XML_WRITER_WRAP: ObjectWriter = xmlMapper.writer()
//            .with(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
//            .without(ToXmlGenerator.Feature.UNWRAP_ROOT_OBJECT_NODE)
//            .with(ToXmlGenerator.Feature.WRITE_XML_1_1)
//        val XML_WRITER_UNWRAP: ObjectWriter = xmlMapper.writer()
//            .with(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
//            .with(ToXmlGenerator.Feature.UNWRAP_ROOT_OBJECT_NODE)
//            .with(ToXmlGenerator.Feature.WRITE_XML_1_1)
//
//        val xml: ObjectNode = xmlMapper.createObjectNode()
//        val child = xml.putObject("root")
//
//
//        val xmlString = XML_WRITER_UNWRAP.writeValueAsString(jsonNode)
//        LOGGER.info(xmlString)
//
//        LOGGER.info(DOCUMENT_BUILDER.get().parse(xmlString).toString())


        return RawMessage.newBuilder().apply {
            parentEventId = message.parentEventId
            metadataBuilder.putAllProperties(message.metadata.propertiesMap)
            metadataBuilder.protocol = XmlPipelineCodecFactory.PROTOCOL
            metadataBuilder.id = message.metadata.id
            metadataBuilder.timestamp = message.metadata.timestamp
            body = ByteString.copyFrom("xmlString", xmlCharset)
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

            val jsonString = U.xmlToJson(xmlString)

            val jsonNode: JsonNode = jsonMapper.readTree(jsonString)

            check(jsonNode.size()==1) {"There more then one root messages after xml to Node process"}

            return messageBuilder.apply {
                messageType = jsonNode.fieldNames().next()
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