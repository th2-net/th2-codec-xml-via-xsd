/*
 * Copyright 2021-2022 Exactpro (Exactpro Systems Limited)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.utils.XmlTest
import com.exactpro.th2.codec.xml.utils.parsedMessage
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.addFields
import com.exactpro.th2.common.message.message
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Disabled for the new version of codec-xml-via-xsd")
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