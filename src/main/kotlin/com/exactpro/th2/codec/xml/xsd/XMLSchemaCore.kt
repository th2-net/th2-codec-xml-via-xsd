package com.exactpro.th2.codec.xml.xsd

import org.apache.ws.commons.schema.*
import java.io.FileInputStream
import javax.xml.namespace.QName
import javax.xml.transform.stream.StreamSource

class XMLSchemaCore {
    private val schemaElements: MutableList<XmlSchemaElement> = mutableListOf()

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
                    val itemElement = item as XmlSchemaElement

                    schemaElements.add(itemElement)

                    addChild(element.qName, MyXmlElement(itemElement), xsdElements)
                    // Call method recursively to get all subsequent element
                    getChildElementNames(itemElement, xsdElements)
                    schemaElements.clear()
                }
            }

        }
    }

    private fun addChild(qName: QName, child: MyXmlElement, xsdElements: MutableMap<QName, MutableList<MyXmlElement>>) {
        val values: MutableList<MyXmlElement> = xsdElements[qName] ?: ArrayList()

        values.add(child)
        xsdElements[qName] = values;
    }

    class MyXmlElement(private val element: XmlSchemaElement) {
        private val type: XmlSchemaType = element.schemaType

        val qName = element.qName

        fun getElementType(): ElementType {
            return when {
                type !is XmlSchemaComplexType -> ElementType.SIMPLE_VALUE
                element.maxOccurs > 1 -> ElementType.LIST_VALUE
                else -> ElementType.MESSAGE_VALUE
            }
        }
    }
}