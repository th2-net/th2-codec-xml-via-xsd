package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.utils.XmlTest
import com.exactpro.th2.codec.xml.utils.parsedMessage
import com.exactpro.th2.common.message.addFields
import com.exactpro.th2.common.message.message
import org.junit.jupiter.api.Test

class XmlNamespaceTest : XmlTest() {

    @Test
    fun `empty namespace does not cause an exception during decoding`() {
        var content = """
            <settlementInstructionPair xmlns=""><test>42</test></settlementInstructionPair>
        """.trimIndent()

        val msg = parsedMessage("settlementInstructionPair").addFields(
            "settlementInstructionPair", message().apply {
                addFields("test", "42")
                addFields("-xmlns", "")
            },
        )

        checkDecode(content, msg)
    }

    @Test
    fun `namespace is added to field name`() {
        var content = """
            <settlementInstructionPair xmlns:t="https://test.com"><t:test>42</t:test></settlementInstructionPair>
        """.trimIndent()

        val msg = parsedMessage("settlementInstructionPair").addFields(
            "settlementInstructionPair", message().apply {
                addFields("t:test", "42")
                addFields("-xmlns:t", "https://test.com")
            },
        )

        checkDecode(content, msg)
    }
}
