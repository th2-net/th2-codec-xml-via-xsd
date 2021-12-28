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
import com.exactpro.th2.codec.xml.utils.ZipBase64Codec
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.google.protobuf.ByteString
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertFailsWith


class XsdTest : XmlTest() {

    @Test
    fun `decode xsd schemas to tmp folder`() {
        val parentDirPath = Path.of("tmp").also {
            Files.createDirectories(it)
        }

        val zipBase64 = Thread.currentThread().contextClassLoader.getResource("XSDset.zip")!!

//        File("xsd_dictionary.txt").apply {
//            createNewFile()
//            writeText(String(encodeFileToBase64Binary(zipBase64.file), StandardCharsets.UTF_8))
//        }

        val xsdMap = ZipBase64Codec.decode(encodeFileToBase64Binary(zipBase64.file), parentDirPath.toFile())
        assertContains(xsdMap, "cafm.001.001.01.xsd")
        assertContains(xsdMap, "cafm.002.001.01.xsd")
        assertContains(xsdMap, "invoice.xsd")
        assertContains(xsdMap, "music_band.xsd")
        assertContains(xsdMap, "registration.xsd")
        assertContains(xsdMap, "service.xsd")
    }

    @Test
    fun `xsd not found exception`() {
        val xml = """
            <Attributes schemaLocation="test.123.123.01 test.xsd" defaultMsgAttrA="123" msgAttrA="45" msgAttrB="67">
                <commonWithAttrs commonAttrA="54" commonAttrB="76">abc</commonWithAttrs>
                <withAttrs defaultFieldAttrA="456" fieldAttrA="10" fieldAttrB="30">def</withAttrs>
            </Attributes>
        """.trimIndent()

        val raw = RawMessage.newBuilder().apply {
            body = ByteString.copyFrom(xml.toByteArray())
        }.build()

        val group = MessageGroup.newBuilder().apply {
            addMessages(AnyMessage.newBuilder().setRawMessage(raw).build())
        }.build()


        assertFailsWith<IllegalStateException> ("Error needed due no xsd for xml validation") {
            codec.decode(group)
        }
    }

}