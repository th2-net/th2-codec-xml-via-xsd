package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.utils.XmlTest
import com.exactpro.th2.codec.xml.utils.parsedMessage
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.addFields
import com.exactpro.th2.common.message.message
import org.junit.jupiter.api.Test

class XmlRepeatingTest : XmlTest() {

    @Test
    fun `test simple repeating group`() {
        val xml = """
            <SimpleRepeating>
              <user id="1">admin</user>
              <user id="2">user</user>
              <user id="3">guest</user>
            </SimpleRepeating>
        """.trimIndent()

        val msg = parsedMessage("SimpleRepeating").addFields(
            "SimpleRepeating", message().apply {
                addField("user", listOf(message().apply {
                    addFields("-id", "1", "#text", "admin")
                }, message().apply {
                    addFields("-id", "2", "#text", "user")
                }, message().apply {
                    addFields("-id", "3", "#text", "guest")
                }))
            }
        )
        checkDecode(xml, msg)
        checkEncode(xml, msg)
    }
}