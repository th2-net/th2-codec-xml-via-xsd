package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.utils.XmlTest
import com.exactpro.th2.codec.xml.utils.parsedMessage
import com.exactpro.th2.common.message.addFields
import org.junit.jupiter.api.Test

class XmlCustomRootTest : XmlTest("TestMessage01") {

    @Test
    fun `test decode custom root`() {
        val xml = """
            <TestMessage01>
                <type>CommonFieldsA</type>
                <f>123</f>
                <abc>
                    <ab>
                        <a>345</a>
                        <b>678</b>
                    </ab>
                    <c>90</c>
                </abc>
            </TestMessage01>
        """

        val json = """{"CommonFieldsA":{"type":"CommonFieldsA","f":"123","abc":{"ab":{"a":"345","b":"678"},"c":"90"}}}"""
        val msg = parsedMessage("CommonFieldsA").addFields(
            "json", json,
        )
        checkDecode(xml, msg)
    }

    @Test
    fun `test encode custom root`() {
        val xml = """
            <TestMessage01>
              <type>CommonFieldsA</type>
              <f>123</f>
              <abc>
                <ab>
                  <a>345</a>
                  <b>678</b>
                </ab>
                <c>90</c>
              </abc>
            </TestMessage01>
        """.trimIndent()

        val json = """{"CommonFieldsA":{"type":"CommonFieldsA","f":"123","abc":{"ab":{"a":"345","b":"678"},"c":"90"}}}"""
        val msg = parsedMessage("CommonFieldsA").addFields(
            "json", json,
        )
        checkEncode(xml, msg)
    }
}