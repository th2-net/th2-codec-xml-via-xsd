package com.exactpro.th2.codec.xml.xsd

import org.apache.ws.commons.schema.XmlSchema
import org.apache.ws.commons.schema.XmlSchemaAny
import org.apache.ws.commons.schema.XmlSchemaChoice
import org.apache.ws.commons.schema.XmlSchemaCollection
import org.apache.ws.commons.schema.XmlSchemaComplexType
import org.apache.ws.commons.schema.XmlSchemaElement
import org.apache.ws.commons.schema.XmlSchemaParticle
import org.apache.ws.commons.schema.XmlSchemaSequence
import org.apache.ws.commons.schema.utils.XmlSchemaObjectBase
import java.io.FileInputStream
import java.util.LinkedList
import java.util.Properties
import javax.xml.namespace.QName
import javax.xml.transform.stream.StreamSource
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class XMLSchemaCore {
    private val cachedURIXsds = LinkedList<String>() // TODO: cache URI xsds here
    val xsdProperties = Properties().also { it.load(Thread.currentThread().contextClassLoader.getResourceAsStream("xsds.properties")) }

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
            addChild(element.qName, XmlElementWrapper(it))
            // Call method recursively to get all subsequent element
            getChildElementNames(it)
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
            is XmlSchemaAny -> emptyList()
            else -> { throw IllegalArgumentException("Not a valid type of $item") }
        }
    }

    private fun MutableMap<QName, MutableList<XmlElementWrapper>>.addChild(qName: QName, child: XmlElementWrapper) {
        val values: MutableList<XmlElementWrapper> = this[qName] ?: ArrayList()

        values.add(child)
        this[qName] = values
    }
}