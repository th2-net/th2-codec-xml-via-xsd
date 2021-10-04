package com.exactpro.th2.codec.xml.xsd

import mu.KotlinLogging
import org.slf4j.Logger
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
        LOGGER.error("[$level] line nr: $lineNumber column nr: $columnNumber \nmessage: $message")
        throw SAXException("[$level] line nr: $lineNumber column nr: $columnNumber \nmessage: $message", exception)
    }

    companion object {
        private val LOGGER: Logger = KotlinLogging.logger {  }
    }
}