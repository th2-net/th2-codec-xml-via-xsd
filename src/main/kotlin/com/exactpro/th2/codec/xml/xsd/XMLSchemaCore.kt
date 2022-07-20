package com.exactpro.th2.codec.xml.xsd

import org.apache.ws.commons.schema.*
import org.apache.ws.commons.schema.utils.XmlSchemaObjectBase
import java.io.FileInputStream
import java.io.FileReader
import java.util.*
import javax.xml.namespace.QName
import javax.xml.transform.stream.StreamSource
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class XMLSchemaCore {
    private val schemaElements: MutableList<XmlSchemaElement> = mutableListOf() // FIXME: what is it for?
    private val cachedURIXsds = LinkedList<String>()
    val xsdProperties = Properties().also { it.load(FileReader("src/main/resources/xsds.properties")) }

    private val xsdElements: MutableMap<QName, MutableList<XmlElementWrapper>> = HashMap()

    fun getXSDElements(xsdPath: String): Map<QName, List<XmlElementWrapper>> {
        val xmlSchemaCollection = XmlSchemaCollection()

        // Schema contains the complete XSD content which needs to be parsed
        val schema: XmlSchema = xmlSchemaCollection.read(StreamSource(FileInputStream(xsdPath)))

        schema.elements.forEach {
            val element = XmlElementWrapper(it.value)
            val qName = it.key

            xsdElements.putIfAbsent(qName, mutableListOf(element))

            // Get all the elements based on the parent element
            val childElement: XmlSchemaElement = xmlSchemaCollection.getElementByQName(qName)

            // Call method to get all the child elements
            xsdElements.getChildElementNames(childElement)
        }

        return xsdElements
    }

    private fun MutableMap<QName, MutableList<XmlElementWrapper>>.getChildElementNames(element: XmlSchemaElement) {
        val elementType = element.schemaType

        if (elementType is XmlSchemaComplexType) {
            val particle: XmlSchemaParticle? = elementType.particle

//            xsdElements.putIfAbsent(element.qName, mutableListOf(XmlElementWrapper(element)))
            xsdElements.putIfAbsent(element.qName, mutableListOf())

            if (particle is XmlSchemaSequence) {
                particle.items.forEach { item ->
                   processItemElements(getItemElements(item), element)
                }
            } else if (particle is XmlSchemaChoice) {
                particle.items.forEach { item ->
                    processItemElements(getItemElements(item), element)
                }
            }
        }
    }

    private fun MutableMap<QName, MutableList<XmlElementWrapper>>.processItemElements(itemElements: Collection<XmlSchemaElement>,
    element: XmlSchemaElement) {
        itemElements.forEach {
            schemaElements.add(it)

            addChild(element.qName, XmlElementWrapper(it))
            // Call method recursively to get all subsequent element
            getChildElementNames(it)
            schemaElements.clear()
        }
    }

    private fun getItemElements(item: XmlSchemaObjectBase): Collection<XmlSchemaElement> {
        return when (item) {
            is XmlSchemaElement -> listOf(item)
            is XmlSchemaChoice -> item.items.mapNotNull {
                if (it is XmlSchemaElement) { it } else { null }
            }
            is XmlSchemaSequence -> item.items.mapNotNull {
                if (it is XmlSchemaElement) { it } else { null }
            }
            is XmlSchemaAny -> {
                val targetNamespace = item.targetNamespace

                if (targetNamespace.startsWith("http") && !cachedURIXsds.contains(targetNamespace)) {
                    cachedURIXsds.add(targetNamespace)
                    // Add all contents to xsdElements and return elements
                    return xsdElements.cacheXsdFromNamespaceURI(item)
                } else {
                    emptyList()
                }
            }
            else -> { throw IllegalArgumentException("Not a valid type of $item") }
        }
    }

    private fun MutableMap<QName, MutableList<XmlElementWrapper>>.cacheXsdFromNamespaceURI(item: XmlSchemaAny): Collection<XmlSchemaElement> {
        val targetNamespace = item.targetNamespace

        val xsdFileName = xsdProperties.getProperty(targetNamespace.substring(7))

        val xmlSchemaCollection = XmlSchemaCollection()

        val schema: XmlSchema = xmlSchemaCollection.read(StreamSource(FileInputStream(xsdFileName)))

        schema.elements.forEach {
            val element = XmlElementWrapper(it.value)
            val qName = it.key

            putIfAbsent(qName, mutableListOf(element))

            // Get all the elements based on the parent element
            val childElement: XmlSchemaElement = xmlSchemaCollection.getElementByQName(qName)

            // Call method to get all the child elements
            getChildElementNames(childElement)
        }

        return schema.elements.values
    }

    private fun MutableMap<QName, MutableList<XmlElementWrapper>>.addChild(qName: QName, child: XmlElementWrapper) {
        val values: MutableList<XmlElementWrapper> = this[qName] ?: ArrayList()

        values.add(child)
        this[qName] = values
    }
}