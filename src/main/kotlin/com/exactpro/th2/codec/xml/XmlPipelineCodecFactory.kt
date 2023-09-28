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
import com.exactpro.th2.codec.api.IPipelineCodecContext
import com.exactpro.th2.codec.api.IPipelineCodecFactory
import com.exactpro.th2.codec.api.IPipelineCodecSettings
import mu.KotlinLogging
import java.io.InputStream
import com.google.auto.service.AutoService
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@AutoService(IPipelineCodecFactory::class)
class XmlPipelineCodecFactory : IPipelineCodecFactory {
    override val settingsClass: Class<out IPipelineCodecSettings> = XmlPipelineCodecSettings::class.java
    override val protocols: Set<String>
        get() = setOf(PROTOCOL)
    private lateinit var context: IPipelineCodecContext
    private val lock = ReentrantLock()
    @Volatile
    private lateinit var xsdMap: Map<String, () -> InputStream>

    override fun init(pipelineCodecContext: IPipelineCodecContext) {
        context = pipelineCodecContext
    }

    override fun create(settings: IPipelineCodecSettings?): IPipelineCodec {
        val codecSettings = settings as? XmlPipelineCodecSettings ?: XmlPipelineCodecSettings()
        return XmlPipelineCodec(codecSettings, initXsdMap(codecSettings))
    }

    private fun initXsdMap(settings: XmlPipelineCodecSettings): Map<String, () -> InputStream> {
        return lock.withLock {
            if (::xsdMap.isInitialized) {
                xsdMap
            } else {
                LOGGER.info { "Loading schemas from settings" }
                settings.schemas.mapValues { (_, alias) ->
                    { context[alias] }
                }.also {
                    xsdMap = it
                }
            }
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
        const val PROTOCOL = "XML"
    }
}