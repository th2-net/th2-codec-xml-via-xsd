package com.exactpro.th2.codec.xml.xsd

import com.exactpro.th2.common.grpc.Value
import org.apache.ws.commons.schema.XmlSchemaComplexType
import org.apache.ws.commons.schema.XmlSchemaElement
import org.apache.ws.commons.schema.XmlSchemaType
import com.exactpro.th2.common.grpc.Value.KindCase.SIMPLE_VALUE
import com.exactpro.th2.common.grpc.Value.KindCase.MESSAGE_VALUE
import com.exactpro.th2.common.grpc.Value.KindCase.LIST_VALUE

data class XmlElementWrapper(val element: XmlSchemaElement) {
    private val type: XmlSchemaType? = element.schemaType //{ "Element ${element.targetQName} has a null schemaType" }

    val qName = element.qName ?: element.targetQName

    val elementType: Value.KindCase = when {
        type !is XmlSchemaComplexType -> SIMPLE_VALUE
        element.maxOccurs > 1 -> LIST_VALUE
        type == null -> MESSAGE_VALUE
        else -> MESSAGE_VALUE
    }

    override fun toString() = "${qName.namespaceURI} - ${qName.localPart}"
}