package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.utils.XmlTest
import com.exactpro.th2.codec.xml.utils.parsedMessage
import com.exactpro.th2.common.grpc.Direction
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.addFields
import com.exactpro.th2.common.message.direction
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.messageType
import com.exactpro.th2.common.message.sequence
import com.exactpro.th2.common.message.sessionAlias
import org.junit.jupiter.api.Test

class XmlAttributeTest : XmlTest() {
    @Test
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