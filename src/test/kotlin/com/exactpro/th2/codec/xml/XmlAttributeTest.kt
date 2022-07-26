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

class XmlAttributeTest : XmlTest() {
    @Test
    @Disabled("Disabled for the new version of codec-xml-via-xsd")
    fun `test attributes fields`() {
        val xml = """<TestAttrMessage>
                <b>
                    <c>
                        <d>
                            <e f="123" g="1">asd</e>
                        </d>
                    </c>
                </b>
                <b>
                    <c>
                        <d>
                            <e f="456" g="2" n="48">fgh</e>
                            <h>A</h>
                        </d>
                    </c>
                </b>
                <b>
                    <c>
                        <d>
                            <e f="789" g="3">fgh</e>
                        </d>
                    </c>
                </b>
            </TestAttrMessage>
        """.trimIndent()

        val msg = parsedMessage("TestAttrMessage").addFields(
            "TestAttrMessage",
            message().apply {
                addField("b", listOf(message().apply {
                    addField("c", message().apply {
                        addField("d", message().apply {
                            addField("e", message().apply {
                                addFields("-f", "123")
                                addFields("-g", "1")
                                addFields("#text", "asd")
                            })
                        })
                    })
                }, message().apply {
                    addField("c", message().apply {
                        addField("d", message().apply {
                            addField("e", message().apply {
                                addFields("-f", "456")
                                addFields("-g", "2")
                                addFields("-n", "48")
                                addFields("#text", "fgh")
                            })
                            addField("h", "A")
                        })
                    })
                }, message().apply {
                    addField("c", message().apply {
                        addField("d", message().apply {
                            addField("e", message().apply {
                                addFields("-f", "789")
                                addFields("-g", "3")
                                addFields("#text", "fgh")
                            })
                        })
                    })
                }))
            },
        )

        checkDecode(
            xml,
            msg
        )
    }

    @Test
    @Disabled("Disabled for the new version of codec-xml-via-xsd")
    fun `test decode attrs in different places`() {
        val xml = """
            <Attributes defaultMsgAttrA="123" msgAttrA="45" msgAttrB="67">
                <commonWithAttrs commonAttrA="54" commonAttrB="76">abc</commonWithAttrs>
                <withAttrs defaultFieldAttrA="456" fieldAttrA="10" fieldAttrB="30">def</withAttrs>
            </Attributes>
        """.trimIndent()

        val msg = parsedMessage("Attributes").addFields(
            "Attributes", message().apply {
                addFields("-defaultMsgAttrA", "123",
                    "-msgAttrA", "45",
                    "-msgAttrB", "67",
                    "commonWithAttrs", message().apply {
                        addField("-commonAttrA", "54")
                        addField("-commonAttrB", "76")
                        addField("#text", "abc")
                    },
                    "withAttrs", message().apply {
                        addField("-defaultFieldAttrA", "456")
                        addField("-fieldAttrA", "10")
                        addField("-fieldAttrB", "30")
                        addField("#text", "def")
                    }
                )
            }
        )
        checkDecode(
            xml,
            msg
        )
    }

    @Test
    @Disabled("Disabled for the new version of codec-xml-via-xsd")
    fun `test encode attrs in different place`() {
        val xml = """
        <Attributes defaultMsgAttrA="123" msgAttrA="45" msgAttrB="67">
          <commonWithAttrs commonAttrA="54" commonAttrB="76">abc</commonWithAttrs>
          <withAttrs defaultFieldAttrA="456" fieldAttrA="10" fieldAttrB="30">def</withAttrs>
        </Attributes>
        """.trimIndent()

        val msg = parsedMessage("Attributes").apply {
            addField("Attributes", message().apply {
                addFields("-defaultMsgAttrA", "123",
                    "-msgAttrA", "45",
                    "-msgAttrB", "67",
                    "commonWithAttrs", message().apply {
                        addField("-commonAttrA", "54")
                        addField("-commonAttrB", "76")
                        addField("#text", "abc")
                    },
                    "withAttrs", message().apply {
                        addField("-defaultFieldAttrA", "456")
                        addField("-fieldAttrA", "10")
                        addField("-fieldAttrB", "30")
                        addField("#text", "def")
                    }
                )
            })
        }
        checkEncode(
            xml,
            msg
        )
    }
}