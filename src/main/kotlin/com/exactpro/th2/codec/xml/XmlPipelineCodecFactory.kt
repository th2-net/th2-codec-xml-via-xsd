/*
 * Copyright 2021-2023 Exactpro (Exactpro Systems Limited)
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
import java.io.InputStream
import com.google.auto.service.AutoService
import java.nio.file.Files
import java.nio.file.Path

@AutoService(IPipelineCodecFactory::class)
class XmlPipelineCodecFactory : IPipelineCodecFactory {
    override val settingsClass: Class<out IPipelineCodecSettings> = XmlPipelineCodecSettings::class.java
    override val protocols: Set<String>
        get() = setOf(PROTOCOL)
    private lateinit var xsdMap: Map<String, Path>

    override fun init(dictionary: InputStream) {
        xsdMap = decodeInputToDictionary(dictionary, XSD_FOLDER)
        if (xsdMap.isEmpty()) {
            throw IllegalArgumentException("No xsd were found from input dictionary!")
        }
    }

    override fun create(settings: IPipelineCodecSettings?): IPipelineCodec {
        return XmlPipelineCodec(settings as? XmlPipelineCodecSettings ?: XmlPipelineCodecSettings(), xsdMap)
    }

    companion object {
        private const val XSD_FOLDER: String = "/tmp/xsd"
        private val LOGGER = KotlinLogging.logger { }
        const val PROTOCOL = "XML"

        fun decodeInputToDictionary(dictionary: InputStream, parentDir: String): Map<String, Path> = dictionary.use {
            val parentDirPath = Path.of(parentDir)
            Files.createDirectory(parentDirPath)
            val xsdDir = Files.createTempDirectory(parentDirPath, "")
            val pathMap = ZipBase64Codec.decode(it.readAllBytes(), xsdDir.toFile())

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