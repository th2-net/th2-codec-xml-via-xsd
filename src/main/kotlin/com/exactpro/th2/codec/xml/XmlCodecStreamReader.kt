/*
 * Copyright 2022-2023 Exactpro (Exactpro Systems Limited)
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

import java.io.ByteArrayInputStream
import java.util.ArrayDeque
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.util.StreamReaderDelegate

class XmlCodecStreamReader<T>(
    body: ByteArray,
    private val messageSupplier: () -> T,
    private val appender: FieldAppender<T>,
)
    : StreamReaderDelegate(
        XML_INPUT_FACTORY.createXMLStreamReader(
            ByteArrayInputStream(body)
        )
), AutoCloseable {
    private lateinit var rootNode: NodeContent<T>
    private lateinit var messageType: String

    private val elements = ArrayDeque<NodeContent<T>>()

    @Throws(XMLStreamException::class)
    override fun next(): Int = super.next().also { eventCode ->
        when (eventCode) {
            START_ELEMENT -> {
                val qName = name
                NodeContent(qName, this, messageSupplier, appender).also { nodeContent ->
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
            END_ELEMENT -> elements.pop().also(NodeContent<*>::release)
        }
    }

    fun getMessage(): T {
        check(elements.isEmpty()) {
            "Some of XML nodes aren't closed ${elements.joinToString { it.name }}"
        }

        return messageSupplier().apply {
            with(appender) {
                appendNode(rootNode.name, rootNode)
            }
        }
    }

    companion object {
        private val XML_INPUT_FACTORY = XMLInputFactory.newInstance()
    }
}