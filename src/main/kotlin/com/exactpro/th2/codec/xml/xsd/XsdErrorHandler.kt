/*
 * Copyright 2021-2024 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.codec.xml.xsd

import io.github.oshai.kotlinlogging.KotlinLogging
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException

class XsdErrorHandler : ErrorHandler {

    @Throws(SAXException::class)
    override fun warning(exception: SAXParseException) {
        handleMessage("Warning", exception)
    }

    @Throws(SAXException::class)
    override fun error(exception: SAXParseException) {
        handleMessage("Error", exception)
    }

    @Throws(SAXException::class)
    override fun fatalError(exception: SAXParseException) {
        handleMessage("Fatal", exception)
    }

    @Throws(SAXException::class)
    private fun handleMessage(level: String, exception: SAXParseException){

        val lineNumber: Int = exception.lineNumber
        val columnNumber: Int = exception.columnNumber
        val message: String? = exception.message
        LOGGER.error { "[$level] line nr: $lineNumber column nr: $columnNumber \nmessage: $message" }
        throw SAXException("[$level] line nr: $lineNumber column nr: $columnNumber \nmessage: $message", exception)
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}