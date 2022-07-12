import com.exactpro.th2.codec.xml.xsd.XMLSchemaCore
import com.exactpro.th2.codec.xml.xsd.XmlElementWrapper
import com.exactpro.th2.common.grpc.ListValue
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.grpc.Value
import mu.KotlinLogging
import java.util.Stack
import javax.xml.namespace.QName
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.util.StreamReaderDelegate
import com.exactpro.th2.common.grpc.Value.KindCase.SIMPLE_VALUE
import com.exactpro.th2.common.grpc.Value.KindCase.MESSAGE_VALUE
import com.exactpro.th2.common.grpc.Value.KindCase.LIST_VALUE
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.set
import com.exactpro.th2.common.message.updateField
import com.exactpro.th2.common.value.add
import com.exactpro.th2.common.value.listValue
import com.exactpro.th2.common.value.toValue
import java.nio.file.Path

class StreamReaderDelegateDecorator(reader: XMLStreamReader,
                                    private val rawMessage: RawMessage,
                                    private val xsdMap: Map<String, Path>,
                                    private val xmlSchemaCore: XMLSchemaCore,
//                                    private val xsdElements: MutableMap<QName, List<XmlElementWrapper>>
                                    ) : StreamReaderDelegate(reader) {
    private val listValueBuilders = mutableMapOf<String, ListValue.Builder>()
    private val messageValueBuilders = mutableMapOf<String, Message.Builder>()

    private val elementStack = Stack<QName>()
    private val messageBuilder = message()
    private val metadataBuilder = messageBuilder.metadataBuilder
    private var foundMsgType = false

    // FIXME: figure out something better
//    private val allElements = xsdElements.values.flatten().associate { it.qName to it.elementType }.toMutableMap()
    private val allElements = mutableMapOf<QName, Value.KindCase>()
    private val xsdElements = mutableMapOf<QName, List<XmlElementWrapper>>()

    @Throws(XMLStreamException::class)
    override fun next(): Int {
        val n: Int = super.next()

        when (n) {
            START_ELEMENT -> {
                val qName = QName(namespaceURI, localName)

                elementStack.push(qName)

                if (namespaceURI.startsWith("http")) {
                    cacheXsdFromNamespaceURI(namespaceURI)
                }

                for (i in 0 until attributeCount) {
                    val attributeName = getAttributeName(i).localPart
                    val attributeValue = getAttributeValue(i)

                    if (attributeName == "schemaLocation") {
                        cacheXsdFromAttribute(attributeValue)
                    }
                }

//                val elements = xsdElements[QName(namespaceURI, localName)]

                when(allElements[qName]) {
                    SIMPLE_VALUE -> {
//                        messageBuilders[localName] = localName.toValue()
                    }
                    MESSAGE_VALUE -> {
                        val builder = message().addField(localName, null) // to be updated later
                            .also {
                            if (attributeCount > 0) {
                                writeAttributes(it)
                            }
                        }

//                        messageBuilders[localName] = builder
//                        messageBuilder[localName] = builder
                        messageValueBuilders[localName] = builder
                    }
                    LIST_VALUE -> {
//                        messageBuilders[localName] = listValue().add(localName.toValue())
//                        messageBuilder[localName] = listValue()
                        listValueBuilders[localName] = listValue()
                    }
                    null -> { throw IllegalArgumentException("There's no element for $qName") }
                    else -> { throw IllegalArgumentException("Element ${allElements[qName]} is not a simpleValue, messageValue or listValue") }
                }

                // TODO: also use pointer
                if (!foundMsgType) {
                    metadataBuilder.messageType = localName
                    foundMsgType = true
                }

            }
            CHARACTERS -> {
                if (text.isNotBlank()) {
                    val element = elementStack.peek()

                   when(allElements[element]) {
                       SIMPLE_VALUE -> {
//                           messageBuilders[element.localPart] = text.toValue()
                           messageBuilder[element.localPart] = text.toValue()
                       }
                       MESSAGE_VALUE -> {
//                           val builder = messageBuilders[element.localPart] as Message.Builder
//                           val builder = messageBuilder[element.localPart] as Message.Builder
//                           builder.updateField(element.localPart) { setSimpleValue(text) }

                           messageValueBuilders[element.localPart]?.updateField(element.localPart) { setSimpleValue(text) }
                       }
                       LIST_VALUE -> {
//                           (messageBuilder[localName]).add(localName.toValue())
                           checkNotNull(listValueBuilders[localName]).add(text.toValue())
                       }
                       else -> { throw IllegalArgumentException("Element is not a simpleValue, messageValue or listValue") }
                   }
               }
            }
            END_ELEMENT -> { elementStack.pop() }
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

        listValueBuilders.forEach {
            messageBuilder[it.key] = it.value
        }

        messageValueBuilders.forEach {
            messageBuilder[it.key] = it.value
        }

        val message = messageBuilder.build()
        messageBuilder.clear()
        listValueBuilders.clear()
        messageValueBuilders.clear()

        return message
    }

    fun clearElements() { elementStack.clear() }

    private fun writeAttributes(localBuilder: Message.Builder) {
        for (i in 0 until attributeCount) {
            localBuilder[getAttributeName(i).localPart] = getAttributeValue(i)
        }
    }

    private fun cacheXsdFromAttribute(attributeValue: String) {
        val xsdFileName = "tmp/" + attributeValue.split(' ')[1]

        xmlSchemaCore.getXSDElements(listOf(xsdFileName)).forEach {
            xsdElements.putIfAbsent(it.key, it.value)
        }

        // FIXME: figure out something better
        xsdElements.values.flatten().associate { it.qName to it.elementType }.forEach {
            allElements.putIfAbsent(it.key, it.value)
        }
    }

    private fun cacheXsdFromNamespaceURI(namespaceURI: String) {
        val props = xmlSchemaCore.xsdProperties

        val xsdFileName = props.getProperty(namespaceURI.substring(7))

        xmlSchemaCore.getXSDElements(listOf(xsdFileName)).forEach {
            xsdElements.putIfAbsent(it.key, it.value)
        }

        // FIXME: figure out something better
        xsdElements.values.flatten().associate { it.qName to it.elementType }.forEach {
            allElements.putIfAbsent(it.key, it.value)
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}