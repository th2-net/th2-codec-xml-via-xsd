package com.exactpro.th2.codec.xml.xsd

import com.exactpro.th2.codec.CodecException
import com.exactpro.th2.codec.xml.utils.ResourceResolver
import com.exactpro.th2.codec.xml.xsd.XsdValidator.Companion.forEach
import com.exactpro.th2.common.grpc.RawMessage
import com.google.protobuf.TextFormat
import mu.KotlinLogging
import org.slf4j.Logger
import org.w3c.dom.Document
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.dom.DOMSource
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator


class XsdValidator(private val xsdMap: Map<String, String>) {

    fun validate(xml: ByteArray) {
        ByteArrayInputStream(xml).use { input ->
            val documentXML = DOCUMENT_BUILDER.get().parse(input)

            val attributes = ArrayList<Node>().apply {
                for (i in 0 until documentXML.documentElement.attributes.length) {
                    add(documentXML.documentElement.attributes.item(i))
                }
                addAllAttributes(documentXML.documentElement.childNodes)
            }

            LOGGER.trace("\nAll attributes that was found:\n")
            attributes.forEach { LOGGER.trace("${it.nodeName}='${it.nodeValue}'") }

            xsdMap.forEach { xsd ->
                val schemaFile = SCHEMA_FACTORY.newSchema(File(xsd.value)) // is it worth for each time??
                val documentXSD: Document = DOCUMENT_BUILDER.get().parse(FileInputStream(xsd.value))


                for (attribute in attributes) {
                    if (attribute.nodeValue == documentXSD.documentElement.getAttribute("targetNamespace")) {

                        val validator: Validator = schemaFile.newValidator().apply {
                            errorHandler = XsdErrorHandler()
                        }
                        val items = documentXML.getElementsByTagNameNS(attribute.nodeValue, "*")
                        validator.validate(DOMSource(items.item(0)))
                        break
                    }
                }
                LOGGER.debug("Validation of raw message with XSD:${xsd.key} finished")
            }
        }
    }

    private fun ArrayList<Node>.addAllAttributes(nodeList: NodeList) {
        nodeList.runCatching {
            filter { it.nodeType == Node.ELEMENT_NODE }.forEach { node ->
                node.attributes.forEach(this@addAllAttributes::add)
                addAllAttributes(node.childNodes)
            }
        }
    }


    companion object {
        private val LOGGER: Logger = KotlinLogging.logger { }

        private val SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).apply {
            errorHandler = XsdErrorHandler()
        }

        private val DOCUMENT_BUILDER: ThreadLocal<DocumentBuilder> = ThreadLocal.withInitial {
            try {
                DocumentBuilderFactory.newInstance().newDocumentBuilder()
            } catch (e: ParserConfigurationException) {
                throw CodecException("Error while initialization. Can not create DocumentBuilderFactory", e)
            }
        }

        private fun NodeList.forEach(func: (Node) -> Unit) {
            for (i in 0 until length) {
                func(item(i))
            }
        }

        private fun NamedNodeMap.forEach(func: (Node) -> Unit) {
            for (i in 0 until length) {
                func(item(i))
            }
        }

        private fun NodeList.filter(condition: (Node) -> Boolean) = mutableListOf<Node>().apply {
            this@filter.forEach { node ->
                if (condition(node)) {
                    this.add(node)
                }
            }
        }
    }

}