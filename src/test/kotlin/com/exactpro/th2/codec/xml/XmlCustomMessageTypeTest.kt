package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.utils.XmlTest
import com.exactpro.th2.codec.xml.utils.parsedMessage
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.addFields
import com.exactpro.th2.common.message.message
import org.junit.jupiter.api.Test

const val ROOT_NAME = "TestMessage01"
const val TYPE_NODE = "type"

class XmlCustomMessageTypeTest : XmlTest("/$ROOT_NAME/$TYPE_NODE") {

    @Test
    fun `test decode custom root`() {
        val xml = """
            <$ROOT_NAME>
                <$TYPE_NODE>CustomType</$TYPE_NODE>
                <f>123</f>
                <abc>
                    <ab>
                        <a>345</a>
                        <b>678</b>
                    </ab>
                    <c>90</c>
                </abc>
            </$ROOT_NAME>
        """

        val msg = parsedMessage("CustomType").addFields(
            ROOT_NAME, message().apply {
                addFields(
                    TYPE_NODE, "CustomType",
                    "f", "123",
                    "abc", message().apply {
                        addField("ab", message().apply {
                            addFields("a", "345", "b", "678")
                        })
                        addField("c", "90")
                    }
                )
            }
        )
        checkDecode(xml, msg)
    }

    @Test
    fun `test encode custom root`() {
        val xml = """
            <$ROOT_NAME>
              <$TYPE_NODE>CustomType</$TYPE_NODE>
              <f>123</f>
              <abc>
                <ab>
                  <a>345</a>
                  <b>678</b>
                </ab>
                <c>90</c>
              </abc>
            </$ROOT_NAME>
        """.trimIndent()

        val msg = parsedMessage("CustomType").addFields(
            ROOT_NAME, message().apply {
                addFields(
                    TYPE_NODE, "CustomType",
                    "f", "123",
                    "abc", message().apply {
                        addField("ab", message().apply {
                            addFields("a", "345", "b", "678")
                        })
                        addField("c", "90")
                    }
                )
            }
        )
        checkEncode(xml, msg)
    }
}