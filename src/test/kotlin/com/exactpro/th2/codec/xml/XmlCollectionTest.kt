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
import org.junit.jupiter.api.Test

class XmlCollectionTest : XmlTest() {

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
        val msg = parsedMessage("TestCollection").addFields(
            "TestCollection", message().apply {
                addField("collection", listOf("1234", "5678"))
                addField("collectionMessage", listOf(message().apply {
                    addField("field0", "1011")
                },message().apply {
                    addField("field0", "1213")
                }))
            },
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
        val msg = parsedMessage("TestCollection").addFields(
            "TestCollection", message().apply {
                addField("collection", listOf("1234", "5678"))
                addField("collectionMessage", listOf(message().apply {
                    addField("field0", "1011")
                },message().apply {
                    addField("field0", "1213")
                }))
            },
        )

        checkEncode(xml, msg)
    }

    @Test
    fun `test decode array with self-closing tag in list element`() {
        val xml = """
            <TestCollection>
                <array/>
                <array>
                    <data>1</data>
                    <data>2</data>
                </array>
            </TestCollection>
        """.trimIndent()
        val msg = parsedMessage("TestCollection").addFields(
            "TestCollection", message().apply {
                addField("array", listOf(message().apply {
                    addField("data", listOf("1", "2"))
                }))
            },
        )

        checkDecode(xml, msg)
    }

    @Test
    fun `test decode array with self-closing tag as single element`() {
        val xml = """
            <TestCollection>
                <array/>
            </TestCollection>
        """.trimIndent()
        val msg = parsedMessage("TestCollection").addFields(
            "TestCollection", message(),
        )

        checkDecode(xml, msg)
    }
}