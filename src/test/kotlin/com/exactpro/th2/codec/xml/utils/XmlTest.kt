package com.exactpro.th2.codec.xml.utils

import com.exactpro.sf.common.messages.structures.IDictionaryStructure
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader
import com.exactpro.th2.codec.api.IPipelineCodec
import com.exactpro.th2.codec.xml.XmlPipelineCodec
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageGroup
import kotlin.test.assertEquals

abstract class XmlTest {

    private val codec: IPipelineCodec

    protected fun checkEncode(xml: String, message: Message.Builder) {
        val group = codec.encode(MessageGroup.newBuilder().addMessages(AnyMessage.newBuilder().setMessage(message)).build())
        assertEquals(1, group.messagesCount)

        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n$xml",
            group.messagesList[0].rawMessage.body.toStringUtf8()
        )
    }

    protected fun checkDecode(xml: String, message: Message.Builder) {
        val group = codec.decode(createRawMessage(xml))
        assertEquals(1, group.messagesCount)

        assertEqualsMessages(message.build(), group.messagesList[0].message, true)
    }

    init {
        codec = XmlPipelineCodec(null)
    }
}