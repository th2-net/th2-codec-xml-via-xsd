package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.utils.XmlTest
import com.exactpro.th2.codec.xml.utils.assertEqualsMessages
import com.exactpro.th2.codec.xml.utils.createRawMessage
import com.exactpro.th2.codec.xml.utils.parsedMessage
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.message.addFields
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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
        val json = """{
  "TestCollection": {
    "collection": [
      "1234",
      "5678"
    ],
    "collectionMessage": [
      {
        "field0": "1011"
      },
      {
        "field0": "1213"
      }
    ]
  }
}"""
        val msg = parsedMessage("TestCollection").addFields(
            "json", json,
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
        val json ="""{
          "TestCollection": {
            "collection": [
              "1234",
              "5678"
            ],
            "collectionMessage": [
              {
                "field0": "1011"
              },
              {
                "field0": "1213"
              }
            ]
          }
        }"""
        val msg = parsedMessage("TestCollection").addFields(
            "json", json,
        )

        checkEncode(xml, msg)
    }
}