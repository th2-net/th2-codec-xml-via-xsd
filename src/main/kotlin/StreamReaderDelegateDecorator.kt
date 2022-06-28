import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageMetadata
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.grpc.RawMessageMetadata
import com.exactpro.th2.common.message.get
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.set
import mu.KotlinLogging
import java.util.Stack
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.util.StreamReaderDelegate

class StreamReaderDelegateDecorator(reader: XMLStreamReader, private val rawMessage: RawMessage) : StreamReaderDelegate(reader) {
    private val map: MutableMap<String, Any?> = mutableMapOf() // key - node name, value - node content
    private val elementStack = Stack<String>()
    private val messageBuilder = message()

    @Throws(XMLStreamException::class)
    override fun next(): Int {
        val n: Int = super.next()

        when (n) {
            XMLStreamConstants.START_ELEMENT -> { elementStack.push(localName) }
            XMLStreamConstants.CHARACTERS -> {
                val element = elementStack.peek()
//                map[element] = textCharacters
                messageBuilder[element] = textCharacters // TODO: how to make nested fields?
//                messageBuilder.get(element).
            }
            XMLStreamConstants.END_ELEMENT -> { elementStack.pop() }
        }

        return n
    }

    fun getMessage(): Message {
        val metadata = rawMessage.metadata

        val metadataBuilder = messageBuilder.metadataBuilder.apply {
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

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}