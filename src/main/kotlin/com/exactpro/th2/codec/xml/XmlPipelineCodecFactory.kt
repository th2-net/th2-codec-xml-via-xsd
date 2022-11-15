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

import com.exactpro.th2.codec.api.IPipelineCodec
import com.exactpro.th2.codec.api.IPipelineCodecContext
import com.exactpro.th2.codec.api.IPipelineCodecFactory
import com.exactpro.th2.codec.api.IPipelineCodecSettings
import com.exactpro.th2.codec.xml.utils.ZipBase64Codec
import com.exactpro.th2.codec.xml.xsd.XsdErrorHandler
import com.exactpro.th2.codec.xml.xsd.XsdValidator
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import javax.xml.XMLConstants
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory

class XmlPipelineCodecFactory : IPipelineCodecFactory {
    override val settingsClass: Class<out IPipelineCodecSettings> = XmlPipelineCodecSettings::class.java
    override val protocol: String = PROTOCOL
    lateinit var xsdMap: Map<String, Schema>

    @Deprecated("Need to fully replace with alias-init method", ReplaceWith("init(IPipelineCodecContext)"), DeprecationLevel.WARNING)
    override fun init(dictionary: InputStream) {
        xsdMap = decodeInputToDictionary(dictionary)
        if (xsdMap.isEmpty()) {
            throw IllegalArgumentException("No xsd were found from decoded archive!")
        }
    }

    override fun init(pipelineCodecContext: IPipelineCodecContext) {
        val aliases = pipelineCodecContext.getDictionaryAliases()

        if (aliases.isEmpty()) {
            return super.init(pipelineCodecContext)
        }

        xsdMap = aliases.associateWith { SCHEMA_FACTORY.newSchema(StreamSource(pipelineCodecContext[it]) as Source)!! }

        LOGGER.info { "Processed alias dictionaries: ${xsdMap.keys.joinToString(", " )}" }
    }

    override fun create(settings: IPipelineCodecSettings?): IPipelineCodec {
        val codecSettings = settings as? XmlPipelineCodecSettings ?: XmlPipelineCodecSettings()
        return XmlPipelineCodec(codecSettings,  XsdValidator(xsdMap, codecSettings.dirtyValidation))
    }

    companion object {
        private const val XSD_FOLDER: String = "./tmp/xsd"
        private val LOGGER = KotlinLogging.logger { }
        const val PROTOCOL = "XML"

        private val SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).apply {
            errorHandler = XsdErrorHandler()
        }

        fun decodeInputToDictionary(archive: InputStream): Map<String, Schema> = archive.use {
            val parentDirPath = Path.of(XSD_FOLDER)
            if (File(parentDirPath.toString()).mkdirs()) {
                LOGGER.warn { "Cannot create dictionary for path: $parentDirPath" }
            }
            val resultMap = ZipBase64Codec.decode(it.readAllBytes(), parentDirPath.toFile()).map { (fileName, path) ->
                fileName to SCHEMA_FACTORY.newSchema(path.toFile())!!
            }.toMap()

            LOGGER.info {
                "Decoded xsd files: ${
                    FileUtils.listFiles(parentDirPath.toFile(), Array(1) {"proto"}, true).map { file ->
                        parentDirPath.relativize(file.toPath())
                    }.toList()
                }"
            }

            resultMap
        }
    }
}