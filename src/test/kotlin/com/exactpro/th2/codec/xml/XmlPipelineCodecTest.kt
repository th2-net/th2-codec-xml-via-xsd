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
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.addFields
import com.exactpro.th2.common.message.message
import com.google.protobuf.ByteString
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class XmlPipelineCodecTest : XmlTest() {

    @Test
    fun `test common decode`() {
        val xml = """
            <CommonFieldsA>
              <f>123</f>
              <abc>
                <ab>
                  <a>345</a>
                  <b>678</b>
                </ab>
                <c>90</c>
              </abc>
            </CommonFieldsA>
        """.trimIndent()

        val msg = parsedMessage("CommonFieldsA").addFields("CommonFieldsA", message().apply {
            addFields("f", "123", "abc", message().apply {
                addField("ab", message().apply {
                    addFields("a", "345", "b", "678")
                })
                addField("c", "90")
            })
        })
        checkDecode(xml, msg)
    }

    @Test
    fun `test common encode`() {
        val xml = """
            <CommonFieldsA>
              <f>123</f>
              <abc>
                <ab>
                  <a>345</a>
                  <b>678</b>
                </ab>
                <c>90</c>
              </abc>
            </CommonFieldsA>
        """.trimIndent()

        val msg = parsedMessage("CommonFieldsA").addFields("CommonFieldsA", message().apply {
            addFields("f", "123", "abc", message().apply {
                addField("ab", message().apply {
                    addFields("a", "345", "b", "678")
                })
                addField("c", "90")
            })
        })
        checkEncode(xml, msg)
    }

    @Test
    fun `test validation of xml declaration`() {
        val withoutValidationCodec = XmlPipelineCodec(XmlPipelineCodecSettings(expectsDeclaration = false), mapOf())
        val withValidationCodec = XmlPipelineCodec(XmlPipelineCodecSettings(expectsDeclaration = true), mapOf())

        // Message with XML declaration
        var xml: MessageGroup = createMessageGroup("""<?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <Msg>
                <Document>123</Document> 
            </Msg>
            """.trimIndent())

        Assertions.assertDoesNotThrow { withoutValidationCodec.decode(xml, reportingContext) }
        Assertions.assertDoesNotThrow { withValidationCodec.decode(xml, reportingContext) }

        // Formatted message with XML declaration
        xml = createMessageGroup("""
            <Msg>
                <Document>123</Document> 
            </Msg>
            """.trimIndent())

        Assertions.assertDoesNotThrow { withoutValidationCodec.decode(xml, reportingContext) }
        Assertions.assertThrows(IllegalStateException::class.java) { withValidationCodec.decode(xml, reportingContext) }
    }

    @Test
    fun `test validation of xml with few root elements`() {
        val withoutValidationCodec = XmlPipelineCodec(XmlPipelineCodecSettings(expectsDeclaration = false), mapOf())

        val xml = createMessageGroup("""<?xml version="1.0" encoding="iso-8859-1" standalone="yes"?>
            <Msg>
                <Document>123</Document> 
            </Msg>
            <Header>
                1234
            </Header>
            """.trimIndent())

        Assertions.assertThrows(IllegalStateException::class.java) {
            withoutValidationCodec.decode(xml, reportingContext)
        }
    }

    private fun createMessageGroup(xmlString: String) = MessageGroup.newBuilder()
        .addMessages(AnyMessage.newBuilder().setRawMessage(RawMessage.newBuilder().apply {
            metadataBuilder.protocol = "XML"
            metadataBuilder.idBuilder.connectionIdBuilder.sessionAlias = "test_session_alias"
            body = ByteString.copyFromUtf8(xmlString)
        }))
        .build()

}