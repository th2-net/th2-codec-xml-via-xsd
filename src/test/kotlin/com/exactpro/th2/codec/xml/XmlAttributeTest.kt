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

        val json =
            """{"TestAttrMessage":{"b":[{"c":{"d":{"e":{"-f":"123","-g":"1","#text":"asd"}}}},{"c":{"d":{"e":{"-f":"456","-g":"2","-n":"48","#text":"fgh"},"h":"A"}}},{"c":{"d":{"e":{"-f":"789","-g":"3","#text":"fgh"}}}}]}}"""
        val msg = parsedMessage("TestAttrMessage").addFields(
            "json", json,
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
        val json =
            """{"Attributes":{"-defaultMsgAttrA":"123","-msgAttrA":"45","-msgAttrB":"67","commonWithAttrs":{"-commonAttrA":"54","-commonAttrB":"76","#text":"abc"},"withAttrs":{"-defaultFieldAttrA":"456","-fieldAttrA":"10","-fieldAttrB":"30","#text":"def"}}}"""
        val msg = parsedMessage("Attributes").addFields(
            "json", json,
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
        val json =
            """{"Attributes":{"-defaultMsgAttrA":"123","-msgAttrA":"45","-msgAttrB":"67","commonWithAttrs":{"-commonAttrA":"54","-commonAttrB":"76","#text":"abc"},"withAttrs":{"-defaultFieldAttrA":"456","-fieldAttrA":"10","-fieldAttrB":"30","#text":"def"}}}"""

        val msg = Message.newBuilder().apply {
            messageType = "Attributes"
            addField("json", json)
        }
        checkEncode(
            xml,
            msg
        )
    }

    @Test
    fun `hard test`() {
        val test_message = message("Acct_001_Message").apply {
            metadataBuilder.apply {
                messageType = "Acct_001_Message"
                sessionAlias = "amqp-broker-3"
                sequence = System.currentTimeMillis()
                direction = Direction.SECOND
            }
            metadataBuilder.protocol = "XML"
            addField("BizMsg", message().apply {
                addField("-xmlns", "urn:asx:xsd:xasx.802.001.04")
                addField("-xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
                addField(
                    "-xsi:schemaLocation",
                    "urn:asx:xsd:xasx.802.001.04 ASX_AU_CHS_comm_802_001_04_xasx_802_001_04.xsd"
                )
                addField("AppHdr", message().apply {
                    addField("-xmlns", "urn:iso:std:iso:20022:tech:xsd:head.001.001.02")
                    addField("-xmlns:n1", "http://www.w3.org/2000/09/xmldsig#")
                    addField("-xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
                    addField(
                        "-xsi:schemaLocation",
                        "urn:iso:std:iso:20022:tech:xsd:head.001.001.02 ASX_AU_CHS_comm_801_001_02_head_001_001_02.xsd"
                    )
                    addField("Fr", message().apply {
                        addField("OrgId", message().apply {
                            addField("Id", message().apply {
                                addField("OrgId", message().apply {
                                    addField("Othr", message().apply {
                                        addField("Id", message().apply {
                                            addField("#text", "01442")
                                        })

                                    })
                                })
                            })
                        })
                    })
                    addField("To", message().apply {
                        addField("OrgId", message().apply {
                            addField("Id", message().apply {
                                addField("OrgId", message().apply {
                                    addField("Othr", message().apply {
                                        addField("Id", message().apply {
                                            addField("#text", "00001")
                                        })

                                    })
                                })
                            })
                        })
                    })
                    addField("BizMsgIdr", message().apply {
                        addField("#text", "01442|2222100")
                    })
                    addField("MsgDefIdr", message().apply {
                        addField("#text", "acmt.001.001.07")
                    })
                    addField("BizSvc", message().apply {
                        addField("#text", "acct_001_001_07_!p")
                    })
                    addField("CreDt", message().apply {
                        addField("#text", "2019-03-07T07:57:21.123Z")
                    })
                    addField("Sgntr", message().apply {
                        addField("#text", "TEST VALUE for Sgntr")
                    })
                })
                addField("Document", message().apply {
                    addField("-xmlns", "urn:iso:std:iso:20022:tech:xsd:acmt.001.001.07")
                    addField("-xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
                    addField(
                        "-xsi:schemaLocation",
                        "urn:iso:std:iso:20022:tech:xsd:acmt.001.001.07 ASX_AU_CHS_acct_001_001_07_acmt_001_001_07.xsd"
                    )
                    addField("AcctOpngInstr", message().apply {
                        addField("MsgId", message().apply {
                            addField("Id", message().apply {
                                addField("#text", "01442|2222200")
                            })
                            addField("CreDtTm", message().apply {
                                addField("#text", "2019-03-07T07:57:21.123Z")
                            })
                        })
                        addField("InstrDtls", message().apply {
                            addField("OpngTp", message().apply {
                                addField("Cd", message().apply {
                                    addField("#text", "NEWA")
                                })
                            })
                        })
                        addField("InvstmtAcct", message().apply {
                            addField("Dsgnt", message().apply {
                                addField("#text", "Account")
                            })
                            addField("Tp", message().apply {
                                addField("Prtry", message().apply {
                                    addField("Id", message().apply {
                                        addField("#text", "SPSD")
                                    })
                                    addField("Issr", message().apply {
                                        addField("#text", "XASX")
                                    })
                                })
                            })
                            addField("OwnrshTp", message().apply {
                                addField("Cd", message().apply {
                                    addField("#text", "JOIT")
                                })
                            })
                        })
                        addField("AcctPties", message().apply {
                            addField("PrncplAcctPty", message().apply {
                                addField("JntOwnr", message().apply {
                                    addField("Pty", message().apply {
                                        addField("Org", message().apply {
                                            addField("Nm", message().apply {
                                                addField("#text", "JANE ANNE BLOGS PTY LTD")
                                            })
                                            addField("LglNttyIdr", message().apply {
                                                addField("#text", "549300KMKZGTL2OYN351")
                                            })
                                            addField("RegnDt", message().apply {
                                                addField("#text", "1957-08-13")
                                            })
                                            addField("PstlAdr", message().apply {
                                                addField("AdrTp", message().apply {
                                                    addField("Cd", message().apply {
                                                        addField("#text", "BIZZ")
                                                    })
                                                })
                                                addField("AdrLine", listOf(
                                                    message().apply { addField("#text", "UNIT1") },
                                                    message().apply { addField("#text", "201-203 BROADWAY AVE") },
                                                    message().apply { addField("#text", "BOND STREET") },
                                                    message().apply { addField("#text", "KOALA BARK DR") },
                                                    message().apply { addField("#text", "WEST BEACH") }
                                                ))
                                                addField("PstCd", message().apply {
                                                    addField("#text", "2564")
                                                })
                                                addField("TwnNm", message().apply {
                                                    addField("#text", "Sydney")
                                                })
                                                addField("Stat", message().apply {
                                                    addField("#text", "NSW")
                                                })
                                                addField("Ctry", message().apply {
                                                    addField("#text", "AU")
                                                })
                                            })
                                            addField("TpOfOrg", message().apply {
                                                addField("Prtry", message().apply {
                                                    addField("Id", message().apply {
                                                        addField("#text", "COMP")
                                                    })
                                                    addField("Issr", message().apply {
                                                        addField("#text", "XASX")
                                                    })
                                                })
                                            })
                                        })
                                    })
                                    addField("PmryComAdr", message().apply {
                                        addField("Email", message().apply {
                                            addField("#text", "holder_janeannebloggs@janemail.com")
                                        })
                                        addField("Mob", message().apply {
                                            addField("#text", "+61-411222333")
                                        })
                                    })
                                })
                            })
                            addField("OthrPty", message().apply {
                                addField("XtndedPtyRole", message().apply {
                                    addField("#text", "NONREF")
                                })
                                addField("OthrPtyDtls", message().apply {
                                    addField("Pty", message().apply {
                                        addField("Org", message().apply {
                                            addField("PstlAdr", message().apply {
                                                addField("AdrLine", listOf(
                                                    message().apply { addField("#text", "UNIT1") },
                                                    message().apply { addField("#text", "201-203 BROADWAY AVE") },
                                                    message().apply { addField("#text", "BOND STREET") }
                                                ))
                                                addField("TwnNm", message().apply {
                                                    addField("#text", "Sydney")
                                                })
                                                addField("Stat", message().apply {
                                                    addField("#text", "NSW")
                                                })
                                                addField("Ctry", message().apply {
                                                    addField("#text", "AU")
                                                })
                                            })
                                        })
                                    })
                                    addField("PmryComAdr", message().apply {
                                        addField("AdrTp", message().apply {
                                            addField("Prtry", message().apply {
                                                addField("Id", message().apply {
                                                    addField("#text", "ELEA")
                                                })
                                                addField("Issr", message().apply {
                                                    addField("#text", "XASX")
                                                })
                                            })
                                        })
                                        addField("Email", message().apply {
                                            addField("#text", "account_janeannebloggs@janemail.com")
                                        })
                                        addField("Mob", message().apply {
                                            addField("#text", "+61-411222333")
                                        })
                                    })
                                    addField("AddtlInf", message().apply {
                                        addField("AddtlInf", message().apply {
                                            addField("#text", "FRGN")
                                        })
                                    })
                                })
                            })
                        })
                    })
                })
            })
        }

        checkEncode("", test_message)
    }
}