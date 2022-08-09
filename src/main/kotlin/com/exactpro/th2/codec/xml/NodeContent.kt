/*
 * Copyright 2022 Exactpro (Exactpro Systems Limited)
 *
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
package com.exactpro.th2.codec.xml

import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageMetadata
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.value.toValue
import javax.xml.namespace.QName

class NodeContent(
    private val nodeName: QName,
    decorator: XmlCodecStreamReader
) {
    private val messageBuilder = Message.newBuilder().apply {
        metadata = MessageMetadata.getDefaultInstance() //FIXME: remove
    }
    private val textSB = StringBuilder()

    private val childNodes: MutableMap<QName, MutableList<NodeContent>> = mutableMapOf()
    private var isMessage: Boolean = false
    private val isEmpty: Boolean
        get() = !isMessage && textSB.isEmpty()

    val name: String = nodeName.toNodeName()

    init {
        decorator.namespaceCount.let { size ->
            if (size > 0) {
                for (i in 0 until size) {
                    decorator.getNamespaceURI(i).also { value ->
                        val prefix = decorator.namespaceContext.getPrefix(value)

                        messageBuilder.addField(makeFieldName(NAMESPACE, prefix, true), value)
                    }
                }
                isMessage = true
            }
        }
        decorator.attributeCount.let { size ->
            if (size > 0) {
                for (i in 0 until size) {
                    val localPart = decorator.getAttributeLocalName(i)
                    val prefix = decorator.getAttributePrefix(i)

                    messageBuilder.addField(makeFieldName(prefix, localPart, true), decorator.getAttributeValue(i))
                }
                isMessage = true
            }
        }
    }

    fun putChild(name: QName, node: NodeContent) {
        childNodes.compute(name) { _, value ->
            value
                ?.apply { add(node) }
                ?: mutableListOf(node)
        }
        isMessage = true
    }

    fun appendText(text: String) {
        if (text.isNotBlank()) {
            textSB.append(text)
        }
    }

    fun release() {
        if (isMessage) {
            childNodes.forEach { (name, values) ->
                val notEmptyValues = values.asSequence().filterNot(NodeContent::isEmpty)

                when (values.size) { // Clarefy type of element: list or single
                    0 -> error("Sub element $name hasn't got values")
                    1 -> messageBuilder.addField(notEmptyValues.first().name, notEmptyValues.first().toValue())
                    else -> messageBuilder.addField(notEmptyValues.first().name, notEmptyValues.map(NodeContent::toValue).toListValue())
                }
            }
            if(textSB.isNotBlank()) {
                messageBuilder.addField(TEXT_FIELD_NAME, textSB.toValue())
            }
        }
    }

    fun toMessage(): Message {
        check(isMessage) {
            "The $nodeName node isn't message"
        }
        return messageBuilder.build()
    }

    override fun toString(): String {
        return "NodeContent(nodeName=$nodeName, childNodes=$childNodes, text=$textSB)"
    }

    private fun Sequence<Value>.toListValue(): Value = Value.newBuilder().apply {
        listValueBuilder.apply {
            forEach(::addValues)
        }
    }.build()

    private fun toValue(): Value = if (isMessage) {
        messageBuilder.toValue()
    } else {
        textSB.toValue()
    }

    companion object {
        private const val NAMESPACE = "xmlns"
        private const val TEXT_FIELD_NAME = "#text"

        private fun QName.toNodeName(): String = makeFieldName(prefix, localPart)

        private fun makeFieldName(first: String, second: String, isAttribute: Boolean = false): String {
            return "${if (isAttribute) "-" else ""}$first${if (first.isNotBlank() && second.isNotBlank()) ":" else ""}$second"
        }
    }
}