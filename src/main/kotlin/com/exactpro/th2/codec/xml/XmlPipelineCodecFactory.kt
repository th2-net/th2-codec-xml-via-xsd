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
import com.exactpro.th2.codec.xml.utils.ZipBase64Codec
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class XmlPipelineCodecFactory : IPipelineCodecFactory {
    override val settingsClass: Class<out IPipelineCodecSettings> = XmlPipelineCodecSettings::class.java
    override val protocol: String = PROTOCOL
    lateinit var xsdMap: Map<String, Path>

    override fun init(dictionary: InputStream) {
        xsdMap = decodeInputDictionary(dictionary, XSD_FOLDER)
        if (xsdMap.isEmpty()) {
            throw IllegalArgumentException("No xsd were found from input dictionary!")
        }
    }

    override fun create(settings: IPipelineCodecSettings?): IPipelineCodec {
        return XmlPipelineCodec(requireNotNull(settings as? XmlPipelineCodecSettings) {
            "settings is not an instance of ${XmlPipelineCodecSettings::class.java}: $settings"
        }, xsdMap)
    }

    companion object {
        private const val XSD_FOLDER: String = "/tmp/xsd"
        private val LOGGER = KotlinLogging.logger { }
        const val PROTOCOL = "XML"

        fun bufferDictionary(inputStream: InputStream) : Map<String, String> {
            val zipXSD = ZipInputStream(inputStream)
            var entryXSD: ZipEntry?
            var nameXSD: String

            File(XSD_FOLDER).mkdirs()

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

        fun decodeInputDictionary(dictionary: InputStream, parentDir: String): Map<String, Path> {
            return dictionary.use {
                val parentDirPath = Path.of(parentDir)
                Files.createDirectories(parentDirPath)
                val xsdDir = Files.createTempDirectory(parentDirPath, "")

                val map = ZipBase64Codec.decode(it.readAllBytes(), xsdDir.toFile())

                LOGGER.info {
                    "Decoded xsd files: ${
                        FileUtils.listFiles(parentDirPath.toFile(), Array(1) {"proto"}, true).map { file -> 
                            parentDirPath.relativize(file.toPath()) 
                        }.toList()
                    }"
                }
                map
            }
        }
    }
}