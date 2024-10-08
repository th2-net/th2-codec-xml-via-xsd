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

import com.exactpro.th2.codec.CodecException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.w3c.dom.Document
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator

class XsdValidator(private val xsdMap: Map<String, () -> InputStream>, private val dirtyValidation: Boolean) {

    fun validate(xml: ByteArray) {
        try {
            ByteArrayInputStream(xml).use { input ->
                val documentXML = DOCUMENT_BUILDER.get().parse(input)

                val attributes = documentXML.findAttributes(SCHEMA_NAME_PROPERTY)

                attributes.forEach { attribute ->
                    val schemas = getSchemas(attribute.nodeValue)
                    schemas.forEach { schema ->
                        val xsdLoader = checkNotNull(xsdMap[schema.value]) { "Cannot find xsd for current `${attribute.nodeName}` attribute: ${attribute.nodeValue}" }

                        val schemaFile = SCHEMA_FACTORY.newSchema(StreamSource(xsdLoader())) // is it worth for each time??

                        val validator: Validator = schemaFile.newValidator().apply {
                            errorHandler = XsdErrorHandler()
                        }

                        val item = documentXML.getElementsByTagNameNS(attribute.nodeValue, "*").item(0)
                        validator.validate(DOMSource(item))
                        LOGGER.debug { "Validation of raw message with XSD: ${schema.value} finished" }
                    }
                }
            }
        } catch (e: Exception) {
            if (dirtyValidation) {
                LOGGER.warn(e) { "VALIDATION ERROR: " }
            } else {
                throw e
            }
        }
    }

    private fun getSchemas(input: String): Map<String, String> = mutableMapOf<String, String>().apply {
        input.split(" ").also {
            check(it.size%2==0) {"schemas must have pairs but had: ${it.size} count"}
            for (i in 0 until it.size-1 step 2) {
                this[it[i]] = it[i+1]
            }
        }
    }

    private fun Document.findAttributes(attributeName: String) = mutableListOf<Node>().apply {
        for (i in 0 until documentElement.attributes.length) {
            val attr = documentElement.attributes.item(i)
            if (attr.nodeName.contains(attributeName)) {
                this.add(attr)
            }
        }
        this.addAllAttributes(documentElement.childNodes, attributeName)
    }

    private fun MutableList<Node>.addAllAttributes(nodeList: NodeList, attributeName: String) {
        runCatching {
            nodeList.filter { it.nodeType == Node.ELEMENT_NODE }.forEach { node ->
                node.attributes.forEach {
                    if (it.nodeName.contains(attributeName)) {
                        this@addAllAttributes.add(it)
                    }
                }
                addAllAttributes(node.childNodes, attributeName)
            }
        }
    }

    companion object {
        private const val SCHEMA_NAME_PROPERTY = "schemaLocation"
        private val LOGGER = KotlinLogging.logger { }

        private val SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).apply {
            errorHandler = XsdErrorHandler()
        }

        val DOCUMENT_BUILDER: ThreadLocal<DocumentBuilder> = ThreadLocal.withInitial {
            try {
                DocumentBuilderFactory.newInstance().apply {
                    isNamespaceAware = true
                }.newDocumentBuilder()
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