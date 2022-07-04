package com.exactpro.th2.codec.xml.xsd

import org.apache.ws.commons.schema.*
import java.io.FileInputStream
import javax.xml.namespace.QName
import javax.xml.transform.stream.StreamSource

class XMLSchemaCore {
    private val schemaElements: MutableList<XmlSchemaElement> = mutableListOf() // FIXME: what is it for?

    fun getXSDElements(xsdPath: String): Map<QName, List<MyXmlElement>> {
        val xmlSchemaCollection = XmlSchemaCollection()
        val xsdElements: MutableMap<QName, MutableList<MyXmlElement>> = HashMap()

        // Schema contains the complete XSD content which needs to be parsed
        val schema: XmlSchema = xmlSchemaCollection.read(StreamSource(FileInputStream(xsdPath)))

        // Get the root element from XSD
        val entry: Map.Entry<QName, XmlSchemaElement> = schema.elements.iterator().next()
        val rootElement: QName = entry.key

        // Get all the elements based on the parent element
        val childElement: XmlSchemaElement = xmlSchemaCollection.getElementByQName(rootElement)

        // Call method to get all the child elements
        getChildElementNames(childElement, xsdElements)

        return xsdElements
    }

    private fun getChildElementNames(element: XmlSchemaElement?, xsdElements: MutableMap<QName, MutableList<MyXmlElement>>) {
        val elementType: XmlSchemaType? = element?.schemaType

        if (elementType is XmlSchemaComplexType) {
            val particle: XmlSchemaParticle? = elementType.particle

            if (particle is XmlSchemaSequence) {
                particle.items.forEach { item ->
                    val itemElements = getItemElements(item)

                   itemElements.forEach {
                       schemaElements.add(it)

                       xsdElements.addChild(element.qName, MyXmlElement(it))
                       // Call method recursively to get all subsequent element
                       getChildElementNames(it, xsdElements)
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

    private fun MutableMap<QName, MutableList<MyXmlElement>>.addChild(qName: QName, child: MyXmlElement) {
        val values: MutableList<MyXmlElement> = this[qName] ?: ArrayList()

        values.add(child)
        this[qName] = values;
    }

    class MyXmlElement(element: XmlSchemaElement) {
        private val type: XmlSchemaType = element.schemaType

        val qName = element.qName

        val elementType: ElementType = when {
                type !is XmlSchemaComplexType -> ElementType.SIMPLE_VALUE
                element.maxOccurs > 1 -> ElementType.LIST_VALUE
                else -> ElementType.MESSAGE_VALUE
            }
    }
}