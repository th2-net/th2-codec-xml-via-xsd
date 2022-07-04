import com.exactpro.th2.codec.xml.xsd.XMLSchemaCore
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.*
import mu.KotlinLogging
import java.nio.file.Path
import java.util.Stack
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.util.StreamReaderDelegate

class StreamReaderDelegateDecorator(reader: XMLStreamReader,
                                    private val rawMessage: RawMessage,
                                    private val xsdMap: Map<String, Path>) : StreamReaderDelegate(reader) {
    private val elementStack = Stack<String>()
    private val messageBuilder = message()
    private val metadataBuilder = messageBuilder.metadataBuilder
    private var foundMsgType = false
    private var previousElement: Int = -1

    private val schemaCore = XMLSchemaCore()
//    private val xsdElements = schemaCore.getXSDElements(xsdMap)

    @Throws(XMLStreamException::class)
    override fun next(): Int {
        val n: Int = super.next()

        when (n) {
            START_ELEMENT -> {
                elementStack.push(localName)

                // TODO: also use pointer
                if (!foundMsgType) {
                    metadataBuilder.messageType = localName
                    foundMsgType = true
                }

                if (previousElement != -1) {
                    val subBuilder = message()
//                    messageBuilder[localName] =

                    if (attributeCount > 0) {
                        writeAttributes()
                    }
                } else {
                    if (attributeCount > 0) {
                        writeAttributes()
                    }
                }

                println("START_ELEMENT localName: $localName")

                previousElement = n
            }
            CHARACTERS -> {
               if (text.isNotBlank()) {
                   val element = elementStack.peek()

                   messageBuilder[element] = text // TODO: how to make nested fields? And what's with listValue?

                   println("CHARACTERS text: $text")
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
        
        val message = messageBuilder.build()
        messageBuilder.clear()

        return message
    }

    fun clearElements() { elementStack.clear() }

    private fun writeAttributes() {
        val builder = message()

        for (i in 0 until attributeCount) {
            println("Attribute $i: ${getAttributeName(i)} - ${getAttributeValue(i)}")

            builder[getAttributeName(i).localPart] = getAttributeValue(i)
        }

        messageBuilder[localName] = builder//.build()
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    private class Element(name: String, builder: Any) {

    }
}