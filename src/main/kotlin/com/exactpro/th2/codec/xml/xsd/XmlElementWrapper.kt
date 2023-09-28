package com.exactpro.th2.codec.xml.xsd

import com.exactpro.th2.common.grpc.Value
import org.apache.ws.commons.schema.XmlSchemaComplexType
import org.apache.ws.commons.schema.XmlSchemaElement
import org.apache.ws.commons.schema.XmlSchemaType
import com.exactpro.th2.common.grpc.Value.KindCase.SIMPLE_VALUE
import com.exactpro.th2.common.grpc.Value.KindCase.MESSAGE_VALUE
import com.exactpro.th2.common.grpc.Value.KindCase.LIST_VALUE

class XmlElementWrapper(element: XmlSchemaElement) {
    private val type: XmlSchemaType? = element.schemaType

    val qName = element.qName ?: element.targetQName

    val elementType: Value.KindCase = when {
        type == null && element.maxOccurs > 1 -> LIST_VALUE
        type == null && element.maxOccurs == 1L -> MESSAGE_VALUE
        type !is XmlSchemaComplexType -> SIMPLE_VALUE
        element.maxOccurs > 1 -> LIST_VALUE
        else -> MESSAGE_VALUE
    }

    override fun toString() = "${qName.namespaceURI} - ${qName.localPart}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as XmlElementWrapper

        if (qName != other.qName) return false
        if (elementType != other.elementType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = qName?.hashCode() ?: 0
        result = 31 * result + elementType.hashCode()
        return result
    }
}