package com.exactpro.th2.codec.xml

import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.RawMessage
import mu.KotlinLogging
import javax.xml.namespace.QName
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.util.StreamReaderDelegate
import com.exactpro.th2.common.message.message
import java.util.Stack

class StreamReaderDelegateDecorator(reader: XMLStreamReader, private val rawMessage: RawMessage) : StreamReaderDelegate(reader) {
    private var messageBuilder = message()

    private var foundMsgType = false

    private val elements = Stack<NodeContent>()

    @Throws(XMLStreamException::class)
    override fun next(): Int {
        val n: Int = super.next()

        when (n) {
            START_ELEMENT -> {
                val qName = QName(namespaceURI, localName, namespaceContext.getPrefix(namespaceURI))

                val nodeContent = NodeContent(qName)
                nodeContent.addAttributes(this)

                if (elements.isNotEmpty()) {
                    val parent = elements.peek()
                    parent.setMessageType()

                    val childNodes = parent.childNodes

                    if (childNodes.contains(qName)) {
                        checkNotNull(childNodes[qName]).add(nodeContent)
                    } else {
                        parent.childNodes[qName] = mutableListOf(nodeContent)
                    }
                }

                elements.push(nodeContent)

                // TODO: also use pointer
                if (!foundMsgType) {
                    messageBuilder.metadataBuilder.messageType = localName
                    foundMsgType = true
                }
            }
            CHARACTERS -> {
                if (text.isNotBlank()) {
                    elements.peek().textSB.append(text)
               }
            }
            END_ELEMENT -> {
                val element = elements.pop()

                if (elements.isEmpty()) {
                    element.release(messageBuilder)
                }
            }
        }

        return n
    }

    fun getMessage(): Message {
        val metadata = rawMessage.metadata
        val metadataBuilder = messageBuilder.metadataBuilder

        metadataBuilder.apply {
            id = metadata.id
            timestamp = metadata.timestamp
            protocol = metadata.protocol
//            messageType = pointer?.let { map.getNode<String>(it) } ?: map.keys.first()
            putAllProperties(metadata.propertiesMap)
        }

        messageBuilder.metadata = metadataBuilder.build()

        if (rawMessage.hasParentEventId()) {
            messageBuilder.parentEventId = rawMessage.parentEventId
        }

        val message = messageBuilder.build()
        messageBuilder.clear()
        metadataBuilder.clear()

        return message
    }

    companion object {
        private val SCHEMA_LOCATION = "schemaLocation"
        private val LOGGER = KotlinLogging.logger { }
    }
}