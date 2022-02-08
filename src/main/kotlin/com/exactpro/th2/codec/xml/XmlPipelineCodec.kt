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
import com.exactpro.th2.codec.xml.utils.toMap
import com.exactpro.th2.codec.xml.utils.toProto
import com.exactpro.th2.codec.xml.xsd.XsdValidator
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.messageType
import com.exactpro.th2.common.message.toJson
import com.github.underscore.lodash.Xml
import com.google.protobuf.ByteString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.nio.file.Path

open class XmlPipelineCodec(private val settings: XmlPipelineCodecSettings, xsdMap: Map<String, Path>)  : IPipelineCodec {

    private val pointer = settings.typePointer?.split("/")?.filterNot { it.isBlank() }
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

        val map = message.toMap()
        val xmlString = Xml.toXml(map)

        validator.validate(xmlString.toByteArray())
        LOGGER.debug("Validation of incoming parsed message complete: ${message.messageType}")

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
            LOGGER.debug("Validation of incoming raw message complete: ${rawMessage.metadata.idOrBuilder}")
            val xmlString = rawMessage.body.toStringUtf8()
            @Suppress("UNCHECKED_CAST")
            val map = Xml.fromXml(xmlString) as MutableMap<String, *>

            LOGGER.debug("Result of the 'Xml.fromXml' method is ${map.keys} for $xmlString")
            map.apply {
                remove(STANDALONE)
                remove(ENCODING)
            }

            if (map.contains(OMIT_XML_DECLARATION)) {
                // U library will tell by this option is there no declaration
                if (!settings.expectsDeclaration || map[OMIT_XML_DECLARATION] == NO) {
                    map.remove("#omit-xml-declaration")
                } else {
                    error("Expecting declaration inside xml data")
                }
            }

            if (map.size > 1) {
                error("There was more than one root node in processed xml, result json have [${map.size}]: ${map.keys.joinToString(", ")}")
            }

            val msgType: String = pointer?.let { map.getNode<String>(it) } ?: map.keys.first()

            return map.toProto(msgType, rawMessage)
        } catch (e: Exception) {
            throw DecodeException("Can not decode message. Can not parse XML. ${rawMessage.body.toStringUtf8()}", e)
        }
    }

    private inline fun <reified T>Map<*,*>.getNode(pointer: List<String>): T {
        val steps = pointer.toMutableList()
        var current = this[steps[0]]
        for (i in 1 until steps.size) {
            if (current == null) {
                error("Can not find element by name: ${steps[i]} in path: $pointer")
            }
            current = (current as Map<*, *>)[steps[i]]
        }
        return current as T
    }


    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(XmlPipelineCodec::class.java)

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