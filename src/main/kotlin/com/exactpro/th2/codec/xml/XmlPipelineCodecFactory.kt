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

import com.exactpro.th2.codec.api.IPipelineCodec
import com.exactpro.th2.codec.api.IPipelineCodecFactory
import com.exactpro.th2.codec.api.IPipelineCodecSettings
import mu.KotlinLogging
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class XmlPipelineCodecFactory : IPipelineCodecFactory {
    override val settingsClass: Class<out IPipelineCodecSettings> = XmlPipelineCodecSettings::class.java
    override val protocol: String = PROTOCOL
    lateinit var xsdMap: Map<String, String>

    override fun init(dictionary: InputStream) {
        xsdMap = bufferDictionary(dictionary)
    }

    override fun create(settings: IPipelineCodecSettings?): IPipelineCodec {
        return XmlPipelineCodec(requireNotNull(settings as? XmlPipelineCodecSettings) {
            "settings is not an instance of ${XmlPipelineCodecSettings::class.java}: $settings"
        }, xsdMap)
    }

    companion object {
        const val PROTOCOL = "XML"
        private const val XSD_FOLDER: String = "DICTIONARY"
        private val LOGGER = KotlinLogging.logger { }

        fun bufferDictionary(inputStream: InputStream) : Map<String, String> {
            val zipXSD = ZipInputStream(inputStream)
            var entryXSD: ZipEntry?
            var nameXSD: String

            File(XSD_FOLDER).mkdir()

            val result = mutableMapOf<String, String>()

            while (zipXSD.nextEntry.also { entryXSD = it } != null) {
                nameXSD = entryXSD!!.name
                val path = "$XSD_FOLDER/$nameXSD"

                LOGGER.trace("XSD: $path")

                val bufferFile = FileOutputStream(path)
                var c: Int = zipXSD.read()
                while (c != -1) {
                    bufferFile.write(c)
                    c = zipXSD.read()
                }
                result[nameXSD] = path
            }

            return result
        }
    }
}