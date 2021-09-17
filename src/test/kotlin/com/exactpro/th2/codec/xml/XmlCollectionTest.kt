package com.exactpro.th2.codec.xml

import com.exactpro.sf.common.messages.structures.IDictionaryStructure
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader
import com.exactpro.th2.codec.xml.utils.assertEqualsMessages
import com.exactpro.th2.codec.xml.utils.createRawMessage
import com.exactpro.th2.codec.xml.utils.parsedMessage
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.message.addFields
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class XmlCollectionTest {

    @Test
    fun `test decode collection`() {
        val xml = """
            <TestCollection>
                <collection>1234</collection>
                <collection>5678</collection>
                <collectionMessage>
                    <field0>1011</field0>
                </collectionMessage>
                <collectionMessage>
                    <field0>1213</field0>
                </collectionMessage>
            </TestCollection>
        """.trimIndent()
        val json = "{\"collection\":[\"1234\",\"5678\"],\"collectionMessage\":[{\"field0\":\"1011\"},{\"field0\":\"1213\"}]}"
        val msg = parsedMessage("TestCollection").addFields(
            "json", json,
        )

        checkDecode(xml, msg)
    }

    @Test
    fun `test encode collection`() {
        val xml = """
            <TestCollection>
                <collection>1234</collection>
                <collection>5678</collection>
                <collectionMessage>
                    <field0>1011</field0>
                </collectionMessage>
                <collectionMessage>
                    <field0>1213</field0>
                </collectionMessage>
            </TestCollection>
        """.trimIndent()
        val json = "{\"collection\":[\"1234\",\"5678\"],\"collectionMessage\":[{\"field0\":\"1011\"},{\"field0\":\"1213\"}]}"
        val msg = parsedMessage("TestCollection").addFields(
            "json", json,
        )

        checkEncode(xml, msg)
    }

    private fun checkEncode(xml: String, message: Message.Builder) {
        val group = codec.encode(MessageGroup.newBuilder().addMessages(AnyMessage.newBuilder().setMessage(message)).build())
        assertEquals(1, group.messagesCount)

        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n$xml\n",
            group.messagesList[0].rawMessage.body.toStringUtf8()
        )
    }

    private fun checkDecode(xml: String, message: Message.Builder) {
        val group = codec.decode(createRawMessage(xml))
        assertEquals(1, group.messagesCount)

        assertEqualsMessages(message.build(), group.messagesList[0].message, true)
    }

    companion object {
        private val dictionary: IDictionaryStructure =
            XmlDictionaryStructureLoader().load(Thread.currentThread().contextClassLoader.getResourceAsStream("test_dictionary.xml"))
        val codec = XmlPipelineCodec(null)

    }
}