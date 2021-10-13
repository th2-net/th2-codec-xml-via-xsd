/*
 * Copyright 2021-2021 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.codec.xml.utils.ZipBase64Codec
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.test.assertContains


class XsdTest {

    @Test
    fun `decode xsd schemas to tmp folder`() {
        val parentDirPath = Path.of("tmp").also {
            Files.createDirectories(it)
        }

        val zipBase64 = Thread.currentThread().contextClassLoader.getResource("XSDset.zip")!!

        val xsdMap = ZipBase64Codec.decode(encodeFileToBase64Binary(zipBase64.file), parentDirPath.toFile())
        assertContains(xsdMap, "cafm.001.001.01.xsd")
        assertContains(xsdMap, "cafm.002.001.01.xsd")
        assertContains(xsdMap, "invoice.xsd")
        assertContains(xsdMap, "music_band.xsd")
        assertContains(xsdMap, "registration.xsd")
        assertContains(xsdMap, "service.xsd")
    }

    @Throws(IOException::class)
    private fun encodeFileToBase64Binary(fileName: String): ByteArray {
        val file = File(fileName)
        return Base64.getEncoder().encode(FileUtils.readFileToByteArray(file))
    }
}