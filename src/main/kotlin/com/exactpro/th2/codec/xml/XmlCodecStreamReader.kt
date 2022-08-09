/*
 * Copyright 2022 Exactpro (Exactpro Systems Limited)
 *
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

import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.direction
import com.exactpro.th2.common.message.getField
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.sequence
import com.exactpro.th2.common.message.sessionAlias
import com.exactpro.th2.common.value.toValue
import com.google.protobuf.TextFormat
import java.io.ByteArrayInputStream
import java.util.*
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.util.StreamReaderDelegate

class XmlCodecStreamReader(
    private val rawMessage: RawMessage,
    private val pointer: List<String>
)
    : StreamReaderDelegate(
        XML_INPUT_FACTORY.createXMLStreamReader(
            ByteArrayInputStream(rawMessage.body.toByteArray())
        )
) {
    private lateinit var rootNode: NodeContent
    private lateinit var messageType: String

    private val elements = Stack<NodeContent>()

    @Throws(XMLStreamException::class)
    override fun next(): Int = super.next().also { eventCode ->
        when (eventCode) {
            START_ELEMENT -> {
                val qName = name
                NodeContent(qName, this).also { nodeContent ->
                    if (elements.isNotEmpty()) {
                        elements.peek()
                            .putChild(qName, nodeContent)
                    }

                    elements.push(nodeContent)

                    if (!this::messageType.isInitialized) {
                        messageType = localName
                    }
                    if (!this::rootNode.isInitialized) {
                        rootNode = nodeContent
                    }
                }
            }
            CHARACTERS -> elements.peek().appendText(text)
            END_ELEMENT -> elements.pop().also(NodeContent::release)
        }
    }

    fun getMessage(): Message {
        check(elements.isEmpty()) {
            "Some of XML nodes of ${TextFormat.shortDebugString(rawMessage.metadata.id)} message aren't closed ${elements.joinToString { it.name }}"
        }

        return message().apply {
            val rawMetadata = rawMessage.metadata

            if (rawMessage.hasParentEventId()) {
                parentEventId = rawMessage.parentEventId
            }

            addField(rootNode.name, rootNode.toMessage().toValue())

            metadataBuilder.apply {
                putAllProperties(rawMetadata.propertiesMap)
                id = rawMetadata.id
                timestamp = rawMetadata.timestamp
                protocol = rawMetadata.protocol
                messageType = extractMessageType(this@XmlCodecStreamReader.messageType)
            }
        }.build()
    }

    private fun Message.Builder.extractMessageType(defaultMessageType: String): String {
        if (pointer.isEmpty()) return defaultMessageType

        var currentNode = this.toValue()
        pointer.forEachIndexed { index, element ->
            check(currentNode.hasMessageValue()) {
                "The `${pointer.take(index)}` node (${currentNode.kindCase}) isn't message in the th2 message $sessionAlias:$direction$sequence"
            }
            currentNode = requireNotNull(currentNode.messageValue.getField(element)) {
                "The `${pointer.take(index + 1)}` element isn't found in message $sessionAlias:$direction$sequence"
            }
        }
        check(currentNode.kindCase == Value.KindCase.SIMPLE_VALUE) {
            "The `$pointer` node (${currentNode.kindCase}) isn't simple value in the th2 message $sessionAlias:$direction$sequence"
        }

        return currentNode.simpleValue
    }

    companion object {
        private val XML_INPUT_FACTORY = XMLInputFactory.newInstance()
    }
}