package com.exactpro.th2.codec.xml.xsd

import org.apache.ws.commons.schema.XmlSchema
import org.apache.ws.commons.schema.XmlSchemaAny
import org.apache.ws.commons.schema.XmlSchemaChoice
import org.apache.ws.commons.schema.XmlSchemaCollection
import org.apache.ws.commons.schema.XmlSchemaComplexType
import org.apache.ws.commons.schema.XmlSchemaElement
import org.apache.ws.commons.schema.XmlSchemaParticle
import org.apache.ws.commons.schema.XmlSchemaSequence
import org.apache.ws.commons.schema.XmlSchemaSequenceMember
import org.apache.ws.commons.schema.XmlSchemaSimpleType
import org.apache.ws.commons.schema.XmlSchemaType
import java.io.FileInputStream
import java.io.FileReader
import java.util.Properties
import javax.xml.namespace.QName
import javax.xml.transform.stream.StreamSource

class XMLSchemaCore {
    private val schemaElements: MutableList<XmlSchemaElement> = mutableListOf() // FIXME: what is it for?
    val xsdProperties = Properties().also { it.load(FileReader("src/main/resources/xsds.properties")) }

    fun getXSDElements(xsdPaths: Collection<String>): Map<QName, List<XmlElementWrapper>> {
        val xmlSchemaCollection = XmlSchemaCollection()
        val xsdElements: MutableMap<QName, MutableList<XmlElementWrapper>> = HashMap()

        xsdPaths.forEach { xsdPath ->
            // Schema contains the complete XSD content which needs to be parsed
            val schema: XmlSchema = xmlSchemaCollection.read(StreamSource(FileInputStream(xsdPath)))

            schema.elements.forEach{ element ->
//                xsdElements[element.key] = mutableListOf(XmlElementWrapper(element.value))
                xsdElements.putIfAbsent(element.key, mutableListOf(XmlElementWrapper(element.value)))

                // Get all the elements based on the parent element
                val childElement: XmlSchemaElement = xmlSchemaCollection.getElementByQName(element.key)

                // Call method to get all the child elements
                xsdElements.getChildElementNames(childElement)
            }

//            // Get the root element from XSD
//            val entry: Map.Entry<QName, XmlSchemaElement> = schema.elements.iterator().next()
//            val rootElement: QName = entry.key
//
//            println("Root entry: ${entry.key}: ${entry.value}")
//
//            xsdElements[rootElement] = mutableListOf(XmlElementWrapper(entry.value))
//
//            // Get all the elements based on the parent element
//            val childElement: XmlSchemaElement = xmlSchemaCollection.getElementByQName(rootElement)
//
//            // Call method to get all the child elements
//            xsdElements.getChildElementNames(childElement)
        }
        xsdElements.forEach { el -> println("XsdElement: ${el.key}: ${el.value}") }

        return xsdElements
    }

    private fun MutableMap<QName, MutableList<XmlElementWrapper>>.getChildElementNames(element: XmlSchemaElement?) {
        val elementType: XmlSchemaType? = element?.schemaType

        if (elementType is XmlSchemaComplexType) {
            val particle: XmlSchemaParticle? = elementType.particle

            if (particle is XmlSchemaSequence) {
                particle.items.forEach { item ->
                    val itemElements = getItemElements(item)

                   itemElements.forEach {
                       schemaElements.add(it)

                       addChild(element.qName, XmlElementWrapper(it))
                       // Call method recursively to get all subsequent element
                       getChildElementNames(it)
                       schemaElements.clear()
                   }
                }
            }
        }
    }

    private fun getItemElements(item: XmlSchemaSequenceMember): Collection<XmlSchemaElement> {
        return when (item) {
            is XmlSchemaElement -> listOf(item)
            is XmlSchemaChoice -> item.items.mapNotNull {
                if (it is XmlSchemaElement) { it } else { null }
            }
            is XmlSchemaSequence -> item.items.map { it as XmlSchemaElement }
            is XmlSchemaAny -> emptyList()
            else -> { throw IllegalArgumentException("Not a valid type of $item") }
        }
    }

    private fun MutableMap<QName, MutableList<XmlElementWrapper>>.addChild(qName: QName, child: XmlElementWrapper) {
        val values: MutableList<XmlElementWrapper> = this[qName] ?: ArrayList()

        values.add(child)
        this[qName] = values;
    }
}