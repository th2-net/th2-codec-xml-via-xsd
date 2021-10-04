package com.exactpro.th2.codec.xml.utils

import com.exactpro.th2.codec.api.IPipelineCodec
import com.exactpro.th2.codec.xml.XmlPipelineCodec
import com.exactpro.th2.codec.xml.XmlPipelineCodecFactory
import com.exactpro.th2.codec.xml.XmlPipelineCodecSettings
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageGroup
import com.google.protobuf.TextFormat
import mu.KotlinLogging
import org.slf4j.Logger
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import kotlin.test.assertEquals

abstract class XmlTest(jsonPathToType: String? = null) {

    private val codec: IPipelineCodec

    protected fun checkEncode(xml: String, message: Message.Builder) {
        val group = codec.encode(MessageGroup.newBuilder().addMessages(AnyMessage.newBuilder().setMessage(message)).build())
        assertEquals(1, group.messagesCount)

        LOGGER.info("ENCODE_RESULT: ${TextFormat.shortDebugString(group)}")

        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n$xml",
            group.messagesList[0].rawMessage.body.toStringUtf8()
        )
    }

    protected fun checkDecode(xml: String, message: Message.Builder) {
        val group = codec.decode(createRawMessage(xml))
        assertEquals(1, group.messagesCount)

        LOGGER.info("DECODE_RESULT: ${TextFormat.shortDebugString(group)}")

        assertEqualsMessages(message.build(), group.messagesList[0].message, true)
    }

    init {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("XSDset.zip")
        val xsdMap = XmlPipelineCodecFactory.bufferDictionary(stream)
        codec = XmlPipelineCodec(XmlPipelineCodecSettings(jsonPathToType), xsdMap)
    }

    companion object {
        private val LOGGER: Logger = KotlinLogging.logger { }
    }
}