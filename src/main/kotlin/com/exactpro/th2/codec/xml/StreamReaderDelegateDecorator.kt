package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.xsd.XMLSchemaCore
import com.exactpro.th2.codec.xml.xsd.XmlElementWrapper
import com.exactpro.th2.common.grpc.ListValue
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageMetadata
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.grpc.Value
import mu.KotlinLogging
import javax.xml.namespace.QName
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.util.StreamReaderDelegate
import com.exactpro.th2.common.grpc.Value.KindCase.KIND_NOT_SET
import com.exactpro.th2.common.grpc.Value.KindCase.MESSAGE_VALUE
import com.exactpro.th2.common.grpc.Value.KindCase.SIMPLE_VALUE
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.set
import com.exactpro.th2.common.value.add
import com.exactpro.th2.common.value.listValue
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class StreamReaderDelegateDecorator(reader: XMLStreamReader,
                                    private val rawMessage: RawMessage,
                                    private val xmlSchemaCore: XMLSchemaCore) : StreamReaderDelegate(reader) {
    private val elementStack = ArrayList<QName>()
    private val elementTypeStack = Stack<Value.KindCase>()

    private val cachedURIXsds = LinkedList<String>()

    private val simpleValueStack = Stack<Value.Builder>()
    private val messageValueStack = Stack<Message.Builder>()
    private val listValueStack = Stack<ListValue.Builder>()

    // key - qName of the parent
    private val msgBuilderWrapperMap = HashMap<QName, MessageBuilderWrapper>()

    private lateinit var messageBuilder: Message.Builder

    private var metadataBuilder = MessageMetadata.newBuilder()

    private var foundMsgType = false

    private val allElements = mutableMapOf<QName, Value.KindCase>()
    private val xsdElements = mutableMapOf<QName, List<XmlElementWrapper>>()

    private val elements = Stack<NodeContent>()

    @Throws(XMLStreamException::class)
    override fun next(): Int {
        val n: Int = super.next()

        when (n) {
            START_ELEMENT -> {
                val qName = QName(namespaceURI, localName, namespaceContext.getPrefix(namespaceURI))

                elementStack.add(qName)

                if (namespaceURI.startsWith("http") && !cachedURIXsds.contains(namespaceURI)) {
                    cacheXsdFromNamespaceURI(namespaceURI)
                    cachedURIXsds.add(namespaceURI)
                }

                for (i in 0 until attributeCount) {
                    val attributeName = getAttributeName(i).localPart
                    val attributeValue = getAttributeValue(i)

                    if (attributeName == SCHEMA_LOCATION) {
                        cacheXsdFromAttribute(attributeValue)
                    }
                }

                val nodeContent = NodeContent(qName)

                if (elements.isNotEmpty()) {
                    val parent = elements.peek()
                    parent.setMessageType()
                    parent.childNodes[qName] = mutableListOf(nodeContent)
                }

                elements.push(nodeContent)

                // TODO: also use pointer
                if (!foundMsgType) {
                    metadataBuilder.messageType = localName
                    foundMsgType = true
                }
            }
            CHARACTERS -> {
                if (text.isNotBlank()) {
                    val qName = elementStack[elementStack.lastIndex]

                    elements.peek().text = text
               }
            }
            END_ELEMENT -> {
//                elementTypeStack.pop()

                val qName = QName(namespaceURI, localName, namespaceContext.getPrefix(namespaceURI))

                val element = elements.pop()

                if (elements.isNotEmpty()) {
                    val parentElement = elements.peek()

                    checkNotNull(parentElement.childNodes[qName]).add(element)
                } else {
//                    println("Element: ${element.childNodes}")
                    val builder = message()
                    element.release(builder)
//                    println("Final message: ${builder.build()}")
                }

                if (elementTypeStack.isNotEmpty()) {
                    val qName = elementStack.removeLast()

                    val parentType = elementTypeStack.peek()

                } else {
//                    messageBuilder = messageValueStack.pop()
                }
            }
        }

        return n
    }

    fun getMessage(): Message {
        val metadata = rawMessage.metadata

        metadataBuilder.apply {
            id = metadata.id
            timestamp = metadata.timestamp
            protocol = metadata.protocol
//            messageType = pointer?.let { map.getNode<String>(it) } ?: map.keys.first()
            putAllProperties(metadata.propertiesMap)
        }

//        messageBuilder.metadata = metadataBuilder.build()
//
//        if (rawMessage.hasParentEventId()) {
//            messageBuilder.parentEventId = rawMessage.parentEventId
//        }
//
//        val message = messageBuilder.build()
//        messageBuilder.clear()
//        msgBuilderWrapperMap.clear()

//        return
        return message().build()
    }

    fun clearElements() { elementStack.clear() }

    private fun writeAttributes(messageBuilder: Message.Builder) {
        for (i in 0 until attributeCount) {
            messageBuilder[getAttributeName(i).localPart] = getAttributeValue(i)
        }
    }

    private fun writeAttributes(listBuilder: ListValue.Builder) {
        val builder = message()

        for (i in 0 until attributeCount) {
            builder.apply {
                this[getAttributeName(i).localPart] = getAttributeValue(i)
            }
        }

        listBuilder.add(builder)
    }

    private fun Message.Builder.addLists(qName: QName) {
        if (msgBuilderWrapperMap.contains(qName)) {
            checkNotNull(msgBuilderWrapperMap[qName]).listBuilderMap.forEach {
                this[it.key.toString()] = it.value
            }
        }
    }

    private fun ListValue.Builder.addLists(qName: QName) {
        if (msgBuilderWrapperMap.contains(qName)) {
            checkNotNull(msgBuilderWrapperMap[qName]).listBuilderMap.forEach {
                add(it.value)
            }
        }
    }

    private fun cacheXsdFromAttribute(attributeValue: String) {
        val xsdFileName = "tmp/" + attributeValue.split(' ')[1]
        val map = mutableMapOf<QName, Value.KindCase>()

        xmlSchemaCore.getXSDElements(xsdFileName).forEach {
            xsdElements.putIfAbsent(it.key, it.value)
        }

        // TODO: figure out something better
        xsdElements.values.flatten().distinct().map { it.qName to it.elementType }
            .forEach { if (!map.containsKey(it.first)) map[it.first] = it.second else map[it.first] = KIND_NOT_SET }

        allElements.putAll(map)
    }

    private fun cacheXsdFromNamespaceURI(namespaceURI: String) {
        val props = xmlSchemaCore.xsdProperties

        val xsdFileName = props.getProperty(namespaceURI.substring(7))
        val map = mutableMapOf<QName, Value.KindCase>()

        xmlSchemaCore.getXSDElements(xsdFileName).forEach {
            xsdElements.putIfAbsent(it.key, it.value)
        }

        // TODO: figure out something better
        xsdElements.values.flatten().distinct().map { it.qName to it.elementType }
            .forEach { if (!map.containsKey(it.first)) map[it.first] = it.second else map[it.first] = KIND_NOT_SET }

        allElements.putAll(map)
    }

    companion object {
        private val SCHEMA_LOCATION = "schemaLocation"
        private val LOGGER = KotlinLogging.logger { }
    }

    private inner class MessageBuilderWrapper(qName: QName, val listBuilderMap: MutableMap<QName, ListValue.Builder> = mutableMapOf()) {
        lateinit var messageBuilder: Message.Builder

        operator fun set(key: QName, value: ListValue.Builder) {
            listBuilderMap[key] = value
        }

        fun contains(key: QName): Boolean = listBuilderMap.contains(key)

        operator fun get(key: QName): ListValue.Builder? = listBuilderMap[key]
    }

}