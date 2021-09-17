package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.utils.XmlTest
import com.exactpro.th2.codec.xml.utils.parsedMessage
import com.exactpro.th2.common.message.addFields
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

        val json = """{"$ROOT_NAME":{"$TYPE_NODE":"CustomType","f":"123","abc":{"ab":{"a":"345","b":"678"},"c":"90"}}}"""
        val msg = parsedMessage("CustomType").addFields(
            "json", json,
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

        val json = """{"$ROOT_NAME":{"$TYPE_NODE":"CustomType","f":"123","abc":{"ab":{"a":"345","b":"678"},"c":"90"}}}"""
        val msg = parsedMessage("AnyType").addFields(
            "json", json,
        )
        checkEncode(xml, msg)
    }
}