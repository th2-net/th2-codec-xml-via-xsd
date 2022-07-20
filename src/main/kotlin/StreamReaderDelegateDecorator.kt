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
import com.exactpro.th2.common.grpc.Value.KindCase.SIMPLE_VALUE
import com.exactpro.th2.common.grpc.Value.KindCase.MESSAGE_VALUE
import com.exactpro.th2.common.grpc.Value.KindCase.LIST_VALUE
import com.exactpro.th2.common.grpc.Value.KindCase.KIND_NOT_SET
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.set
import com.exactpro.th2.common.value.add
import com.exactpro.th2.common.value.listValue
import com.exactpro.th2.common.value.toValue
import java.nio.file.Path
import java.util.*
import kotlin.collections.ArrayList

class StreamReaderDelegateDecorator(reader: XMLStreamReader,
                                    private val rawMessage: RawMessage,
                                    private val xsdMap: Map<String, Path>,
                                    private val xmlSchemaCore: XMLSchemaCore,
                                    ) : StreamReaderDelegate(reader) {
    private val elementStack = ArrayList<QName>()
    private val elementTypeStack = Stack<Value.KindCase>()

    private val cachedURIXsds = LinkedList<String>()

    private val simpleValueStack = Stack<Value.Builder>()
    private val messageValueStack = Stack<Message.Builder>()
    private val listValueStack = Stack<ListValue.Builder>()

    private lateinit var messageBuilder: Message.Builder

    private var metadataBuilder = MessageMetadata.newBuilder()

    private var foundMsgType = false

    private val allElements = mutableMapOf<QName, Value.KindCase>()
    private val xsdElements = mutableMapOf<QName, List<XmlElementWrapper>>()

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

                    if (attributeName == "schemaLocation") {
                        cacheXsdFromAttribute(attributeValue)
                    }
                }

                when(allElements[qName]) {
                    SIMPLE_VALUE -> {
                        elementTypeStack.push(SIMPLE_VALUE)
                    }
                    MESSAGE_VALUE -> {
                        elementTypeStack.push(MESSAGE_VALUE)

//                        val builder = message().addField(localName, null)
                        val builder = message()

                        if (attributeCount > 0) {
                            writeAttributes(builder)
                        }

                        messageValueStack.push(builder)
                    }
                    LIST_VALUE -> {
                        val builder = listValue()

                        if (attributeCount > 0) {
                            writeAttributes(builder)
                        }

                        elementTypeStack.push(LIST_VALUE)
                        listValueStack.push(builder)
                    }
                    KIND_NOT_SET -> {
                        val element = checkNotNull(xsdElements[elementStack[elementStack.lastIndex - 1]]?.find { it.qName == qName })

                        when (element.elementType) {
                            SIMPLE_VALUE -> {
                                elementTypeStack.push(SIMPLE_VALUE)
                            }
                            MESSAGE_VALUE -> {
                                elementTypeStack.push(MESSAGE_VALUE)

//                                val builder = message().addField(localName, null)
                                val builder = message()

                                if (attributeCount > 0) {
                                    writeAttributes(builder)
                                }

                                messageValueStack.push(builder)
                            }
                            LIST_VALUE -> {
                                val builder = listValue()

                                if (attributeCount > 0) {
                                    writeAttributes(builder)
                                }

                                elementTypeStack.push(LIST_VALUE)
                                listValueStack.push(builder)
                            }
                            else -> { throw IllegalArgumentException("Element $qName is not a simpleValue, messageValue or listValue") }
                        }
                    }
                    null -> { throw IllegalArgumentException("There's no element for $qName") }
                    else -> { throw IllegalArgumentException("Element $qName is not a simpleValue, messageValue or listValue") }
                }

                // TODO: also use pointer
                if (!foundMsgType) {
                    metadataBuilder.messageType = localName
                    foundMsgType = true
                }
            }
            CHARACTERS -> {
                if (text.isNotBlank()) {
                    val elementName = elementStack[elementStack.lastIndex]
                    val localElementName = elementName.localPart

                    when(allElements[elementName]) {
                       SIMPLE_VALUE -> {
                           simpleValueStack.add(Value.newBuilder().setSimpleValue(text))
                       }
                       MESSAGE_VALUE -> {
                           val builder = messageValueStack.peek()
//                           builder.updateField(localElementName) { setSimpleValue(text) }
                           builder[localElementName] = text.toValue()
                       }
                       LIST_VALUE -> { // FIXME: mb it's not possible
                           val builder = listValueStack.peek()

                           builder.add(text.toValue())
                       }
                        KIND_NOT_SET -> {
                            val element = checkNotNull(xsdElements[elementStack[elementStack.lastIndex - 1]]?.find { it.qName == elementName })

                            when (element.elementType) {
                                SIMPLE_VALUE -> {
                                    simpleValueStack.add(Value.newBuilder().setSimpleValue(text))
                                }
                                MESSAGE_VALUE -> {
                                    val builder = messageValueStack.peek()
//                                    builder.updateField(localElementName) { setSimpleValue(text) }
                                    builder[localElementName] = text.toValue()
                                }
                                LIST_VALUE -> { // FIXME: mb it's not possible
                                    val builder = listValueStack.peek()

                                    builder.add(text.toValue())
                                }
                                else -> { throw IllegalArgumentException("Element is not a simpleValue, messageValue or listValue") }
                            }
                        }
                       else -> { throw IllegalArgumentException("Element is not a simpleValue, messageValue or listValue") }
                   }
               }
            }
            END_ELEMENT -> {
                elementTypeStack.pop()

                if (elementTypeStack.isNotEmpty()) {
                    val elementName = elementStack.removeLast()

                    val parentType = elementTypeStack.peek()

                    when(allElements[elementName]) {
                        SIMPLE_VALUE -> {
                            if (parentType == MESSAGE_VALUE) {
                                messageValueStack.peek()[elementName.localPart] = simpleValueStack.pop()
                            } else {
                                listValueStack.peek().add(simpleValueStack.pop())
                            }
                        }
                        MESSAGE_VALUE -> {
                            if (parentType == MESSAGE_VALUE) {
                                val message = messageValueStack.pop()
                                messageValueStack.peek()[elementName.localPart] = message
                            } else {
                                listValueStack.peek().add(messageValueStack.pop())
                            }
                        }
                        LIST_VALUE -> {
                            if (parentType == MESSAGE_VALUE) {
                                messageValueStack.peek()[elementName.localPart] = listValueStack.pop()
                            } else {
                                val list = listValueStack.pop()
                                listValueStack.peek().add(list)
                            }
                        }
                        KIND_NOT_SET -> {
                            val element = checkNotNull(
                                xsdElements[elementStack[elementStack.lastIndex]]?.find { it.qName == elementName }
                            ) { "Element $elementName is not found" }

                            when (element.elementType) {
                                SIMPLE_VALUE -> {
                                    if (parentType == MESSAGE_VALUE) {
                                        messageValueStack.peek()[elementName.localPart] = simpleValueStack.pop()
                                    } else {
                                        listValueStack.peek().add(simpleValueStack.pop())
                                    }
                                }
                                MESSAGE_VALUE -> {
                                    if (parentType == MESSAGE_VALUE) {
                                        val message = messageValueStack.pop()
                                        messageValueStack.peek()[elementName.localPart] = message
                                    } else {
                                        listValueStack.peek().add(messageValueStack.pop())
                                    }
                                }
                                LIST_VALUE -> {
                                    if (parentType == MESSAGE_VALUE) {
                                        messageValueStack.peek()[elementName.localPart] = listValueStack.pop()
                                    } else {
                                        val list = listValueStack.pop()
                                        listValueStack.peek().add(list)
                                    }
                                }
                                else -> { throw IllegalArgumentException("Element $elementName is not a simpleValue, messageValue or listValue") }
                            }
                        }
                        else -> { throw IllegalArgumentException("Element $elementName is not a simpleValue, messageValue or listValue") }
                    }
                } else {
                    messageBuilder = messageValueStack.pop()
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

        messageBuilder.metadata = metadataBuilder.build()

        if (rawMessage.hasParentEventId()) {
            messageBuilder.parentEventId = rawMessage.parentEventId
        }

        // TODO: put builders in messageBuilder

        val message = messageBuilder.build()
        messageBuilder.clear()

        return message
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

    private fun cacheXsdFromAttribute(attributeValue: String) {
        val xsdFileName = "tmp/" + attributeValue.split(' ')[1]

        xmlSchemaCore.getXSDElements(xsdFileName).forEach {
            xsdElements.putIfAbsent(it.key, it.value)
        }

        allElements.clear()

        // FIXME: figure out something better
        xsdElements.values.flatten().distinct().map { it.qName to it.elementType }
            .forEach { if (!allElements.containsKey(it.first)) allElements[it.first] = it.second else allElements[it.first] = KIND_NOT_SET }
    }

    private fun cacheXsdFromNamespaceURI(namespaceURI: String) {
        val props = xmlSchemaCore.xsdProperties

        val xsdFileName = props.getProperty(namespaceURI.substring(7))

        xmlSchemaCore.getXSDElements(xsdFileName).forEach {
            xsdElements.putIfAbsent(it.key, it.value)
        }

        allElements.clear()

        // FIXME: figure out something better
        xsdElements.values.flatten().distinct().map { it.qName to it.elementType }
            .forEach { if (!allElements.containsKey(it.first)) allElements[it.first] = it.second else allElements[it.first] = KIND_NOT_SET }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}