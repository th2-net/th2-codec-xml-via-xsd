package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.utils.XmlTest
import com.exactpro.th2.codec.xml.utils.parsedMessage
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.addFields
import com.exactpro.th2.common.message.messageType
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

        val json = """{
  "TestAttrMessage": {
    "b": [
      {
        "c": {
          "d": {
            "e": {
              "-f": "123",
              "-g": "1",
              "#text": "asd"
            }
          }
        }
      },
      {
        "c": {
          "d": {
            "e": {
              "-f": "456",
              "-g": "2",
              "-n": "48",
              "#text": "fgh"
            },
            "h": "A"
          }
        }
      },
      {
        "c": {
          "d": {
            "e": {
              "-f": "789",
              "-g": "3",
              "#text": "fgh"
            }
          }
        }
      }
    ]
  }
}"""
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
        val json = """{
  "Attributes": {
    "-defaultMsgAttrA": "123",
    "-msgAttrA": "45",
    "-msgAttrB": "67",
    "commonWithAttrs": {
      "-commonAttrA": "54",
      "-commonAttrB": "76",
      "#text": "abc"
    },
    "withAttrs": {
      "-defaultFieldAttrA": "456",
      "-fieldAttrA": "10",
      "-fieldAttrB": "30",
      "#text": "def"
    }
  }
}"""
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
            <attributes defaultMsgAttrA="123" msgAttrA="45" msgAttrB="67">
                <commonWithAttrs commonAttrA="54" commonAttrB="76">abc</commonWithAttrs>
                <withAttrs fieldAttrA="10" fieldAttrB="30">def</withAttrs>
            </attributes>
        """.trimIndent()
        val json = "{\"defaultMsgAttrA\":\"123\",\"msgAttrA\":\"45\",\"msgAttrB\":\"67\",\"commonWithAttrs\":{\"commonAttrA\":\"54\",\"commonAttrB\":\"76\",\"\":\"abc\"},\"withAttrs\":{\"defaultFieldAttrA\":\"456\",\"fieldAttrA\":\"10\",\"fieldAttrB\":\"30\",\"\":\"def\"}}"
        val msg = Message.newBuilder().apply {
            messageType = "Attributes"
            addField("json", json)
        }
        checkEncode(
            xml,
            msg
        )
    }
}