/*
 * Copyright 2022-2023 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.value.toValue
import javax.xml.namespace.QName

typealias FieldName = String

interface FieldAppender<T> {
    fun T.appendSimple(name: FieldName, value: String)

    fun T.appendNode(name: FieldName, node: NodeContent<T>)

    fun T.appendNodeCollection(name: FieldName, nodes: List<NodeContent<T>>)
}

class NodeContent<T>(
    private val nodeName: QName,
    decorator: XmlCodecStreamReader<T>,
    messageSupplier: () -> T,
    private val appender: FieldAppender<T>,
) {
    private val messageBuilder: T by lazy(messageSupplier)
    private val textSB = StringBuilder()

    private val childNodes: MutableMap<QName, MutableList<NodeContent<T>>> = mutableMapOf()
    var isMessage: Boolean = false
        private set
    private val isEmpty: Boolean
        get() = !isMessage && textSB.isEmpty()

    val name: String = nodeName.toNodeName()

    init {
        decorator.namespaceCount.let { size ->
            if (size > 0) {
                for (i in 0 until size) {
                    // value can be null (e.g. xmlns="")
                    val value: String = decorator.getNamespaceURI(i) ?: ""
                    val prefix = decorator.namespaceContext.getPrefix(value) ?: ""

                    with(appender) {
                        messageBuilder.appendSimple(makeFieldName(NAMESPACE, prefix, true), value)
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

                    with(appender) {
                        messageBuilder.appendSimple(makeFieldName(prefix, localPart, true), decorator.getAttributeValue(i))
                    }
                }
                isMessage = true
            }
        }
    }

    fun putChild(name: QName, node: NodeContent<T>) {
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
                try {
                    val notEmptyValues = values.filterNot(NodeContent<*>::isEmpty)

                    if (notEmptyValues.isNotEmpty()) {
                        val first = notEmptyValues.first()
                        with(appender) {
                            when (values.size) { // Clarify type of element: list or single
                                0 -> error("Sub element $name hasn't got values")
                                1 -> messageBuilder.appendNode(first.name, first)
                                else -> messageBuilder.appendNodeCollection(
                                    first.name,
                                    notEmptyValues,
                                )
                            }
                        }
                    }
                } catch (e: RuntimeException) {
                    throw IllegalStateException("The `$name` field can't be released in the `$nodeName` node", e)
                }
            }
            if(textSB.isNotBlank()) {
                with(appender) {
                    messageBuilder.appendSimple(TEXT_FIELD_NAME, textSB.toString())
                }
            }
        }
    }

    fun toMessage(): T {
        check(isMessage) {
            "The $nodeName node isn't message"
        }
        return messageBuilder
    }

    fun toText(): String {
        check(!isMessage) {
            "The $nodeName is a message"
        }
        return textSB.toString()
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