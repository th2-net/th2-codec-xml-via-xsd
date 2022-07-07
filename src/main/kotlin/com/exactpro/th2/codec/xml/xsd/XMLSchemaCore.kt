package com.exactpro.th2.codec.xml.xsd

import org.apache.ws.commons.schema.*
import java.io.FileInputStream
import java.io.FileReader
import java.util.Properties
import javax.xml.namespace.QName
import javax.xml.transform.stream.StreamSource

class XMLSchemaCore {
    private val schemaElements: MutableList<XmlSchemaElement> = mutableListOf() // FIXME: what is it for?
    val xsdProperties = Properties().also { it.load(FileReader("xsds.properties")) }

    fun getXSDElements(xsdPaths: Collection<String>): Map<QName, List<XmlElementWrapper>> {
        val xmlSchemaCollection = XmlSchemaCollection()
        val xsdElements: MutableMap<QName, MutableList<XmlElementWrapper>> = HashMap()

        xsdPaths.forEach { xsdPath ->
            // Schema contains the complete XSD content which needs to be parsed
            val schema: XmlSchema = xmlSchemaCollection.read(StreamSource(FileInputStream(xsdPath)))

            // TODO: make schema of URI

            // Get the root element from XSD
            val entry: Map.Entry<QName, XmlSchemaElement> = schema.elements.iterator().next()
            val rootElement: QName = entry.key

            // Get all the elements based on the parent element
            val childElement: XmlSchemaElement = xmlSchemaCollection.getElementByQName(rootElement)

            // Call method to get all the child elements
            xsdElements.getChildElementNames(childElement)
        }

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
        } else if (elementType is XmlSchemaSimpleType) {

        }
    }

    private fun getItemElements(item: XmlSchemaSequenceMember): Collection<XmlSchemaElement> {
        return when (item) {
            is XmlSchemaElement -> listOf(item)
            is XmlSchemaChoice -> item.items.map { it as XmlSchemaElement }
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