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
import com.exactpro.th2.codec.xml.xsd.XsdValidator
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.w3c.dom.Document
import org.xml.sax.SAXException
import java.io.File
import java.io.FileInputStream
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
        private const val XSD_NAMESPACE_ATTRIBUTE: String = "targetNamespace"
        private val LOGGER = KotlinLogging.logger { }
        const val PROTOCOL = "XML"

        fun decodeInputDictionary(dictionary: InputStream, parentDir: String): Map<String, Path> {
            return dictionary.use {
                val parentDirPath = Path.of(parentDir)
                Files.createDirectories(parentDirPath)
                val xsdDir = Files.createTempDirectory(parentDirPath, "")
                val pathMap = ZipBase64Codec.decode(it.readAllBytes(), xsdDir.toFile())

//                val xmlnsMap = mutableMapOf<String, Path>()
//                pathMap.forEach { xsd ->
//                    val xsdFile = xsd.value.toFile()
//                    val documentXSD: Document = XsdValidator.DOCUMENT_BUILDER.get().parse(FileInputStream(xsdFile))
//                    documentXSD.documentElement.getAttribute(XSD_NAMESPACE_ATTRIBUTE)?.also { namespace ->
//                        if(xmlnsMap.contains(namespace)) {
//                            throw SAXException("More than one xsd for key '$namespace' | File names: ${xmlnsMap[namespace]?.fileName}, ${xsd.value.fileName}")
//                        }
//                        xmlnsMap[namespace] = xsd.value
//                    } ?: throw SAXException("Cannot find attribute '$XSD_NAMESPACE_ATTRIBUTE' in xsd '${xsd.key}'")
//                }

                LOGGER.info {
                    "Decoded xsd files: ${
                        FileUtils.listFiles(parentDirPath.toFile(), Array(1) {"proto"}, true).map { file -> 
                            parentDirPath.relativize(file.toPath()) 
                        }.toList()
                    }"
                }
                pathMap
            }
        }
    }
}