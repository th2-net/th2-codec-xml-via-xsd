package com.exactpro.th2.codec.xml

import com.exactpro.th2.common.grpc.ListValue
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.grpc.Value.KindCase.SIMPLE_VALUE
import com.exactpro.th2.common.grpc.Value.KindCase.MESSAGE_VALUE
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.value.add
import com.exactpro.th2.common.value.listValue
import javax.xml.namespace.QName

class NodeContent(val nodeName: QName) {
    private val attributes: MutableMap<String, String> = mutableMapOf()
    val childNodes: MutableMap<QName, MutableList<NodeContent>> = mutableMapOf()

    var textSB: StringBuilder = StringBuilder()
    var type: Value.KindCase = SIMPLE_VALUE

    fun addAttributes(decorator: StreamReaderDelegateDecorator) {
        if (decorator.attributeCount > 0) {
            for (i in 0 until decorator.namespaceCount) {
                val namespace = "xmlns"
                val prefix = decorator.namespaceContext.getPrefix(decorator.getNamespaceURI(i))

                attributes["$namespace${if (prefix.isNotBlank()) ":$prefix" else ""}"] = decorator.getNamespaceURI(i)
            }

            for (i in 0 until decorator.attributeCount) {
                val localPart = decorator.getAttributeLocalName(i)
                val prefix = decorator.getAttributePrefix(i)

                attributes["$prefix:$localPart"] = decorator.getAttributeValue(i)
            }
        }
    }

    fun setMessageType() {
        if (this.type == SIMPLE_VALUE) {
            this.type = MESSAGE_VALUE
        }
    }

    fun release(messageBuilder: Message.Builder) {
        messageBuilder.addNode(nodeName, mutableListOf(this))
    }

    private fun Message.Builder.addNode(nodeName: QName, nodeList: MutableList<NodeContent>) {
        val count = nodeList.count()

        val list = listValue()

        nodeList.forEach { node ->
            when (node.type) {
                MESSAGE_VALUE -> {
                    if (count > 1) {

                        val attributes = getAttributes(node)
                        var attrsAdded = false

                        node.childNodes.forEach {
                            val subMessage = message()

                            if (!attrsAdded) {
                                attributes.forEach { (key, value) -> subMessage.addField(key, value) }
                                attrsAdded = true
                            }

                            list.addNode(subMessage, it.value)
                            list.add(subMessage)
                            addField(toNodeName(nodeName), list)
                        }

                    } else if (count == 1) {
                        val message = message()
                        message.writeAttributes(node)

                        node.childNodes.forEach {
                            message.addNode(it.key, it.value)
                        }

                        addField(toNodeName(nodeName), message)
                    }
                }

                // TODO: mb I it's possible to have a list of simple values too
                SIMPLE_VALUE -> {
                    writeAttributes(node)
                    val text = node.textSB.toString()

                    if (text.isNotBlank()) {
                        addField(toNodeName(nodeName), text)
                    }
                }

                else -> throw IllegalArgumentException("Node $node can be either MESSAGE_VALUE or SIMPLE_VALUE")
            }
        }
    }

    private fun ListValue.Builder.addNode(messageBuilder: Message.Builder, nodeList: MutableList<NodeContent>) {
        val count = nodeList.count()

        val message = message()

        nodeList.forEach { node ->
            message.writeAttributes(node)

            when (node.type) {
                MESSAGE_VALUE -> {

                    if (count > 1) {
                        val list = listValue()

                        node.childNodes.forEach {
                            message.addNode(it.key, it.value)
                            messageBuilder.addField(toNodeName(it.key), message)
                        }

                        add(list)
                    } else if (count == 1) {
                        node.childNodes.forEach {
                            val subMessage = message()
                            subMessage.addNode(it.key, it.value)
                            message.addField(toNodeName(it.key), subMessage)
                        }

                        messageBuilder.addField(toNodeName(node.nodeName), message)
                    }
                }

                // TODO: mb I it's possible to have a list of simple values too
                SIMPLE_VALUE -> {
                    val subMessage = message()
                    subMessage.writeAttributes(node)
                    messageBuilder.addField(toNodeName(node.nodeName), subMessage)

                    val text = node.textSB.toString()

                    if (text.isNotBlank()) {
                        messageBuilder.addField(toNodeName(node.nodeName), text)
                    }
                }

                else -> throw IllegalArgumentException("Node $node can be either MESSAGE_VALUE or SIMPLE_VALUE")
            }
        }
    }
    
    private fun toNodeName(qName: QName): String {
        val prefix = qName.prefix
        val localPart = qName.localPart

        return if (prefix.isNotBlank()) {
            "$prefix:$localPart"
        } else {
            localPart
        }
    }

    override fun toString(): String {
        return "NodeContent(nodeName=$nodeName, attributes=$attributes, childNodes=$childNodes, text=$textSB, type=$type)"
    }

    companion object {
        private fun Message.Builder.writeAttributes(nodeContent: NodeContent) {
            nodeContent.attributes.forEach {
                    addField(it.key, it.value)
            }
        }

        private fun getAttributes(nodeContent: NodeContent): HashMap<String, String> {
            val res = HashMap<String, String>()

            nodeContent.attributes.forEach {
                res[it.key] = it.value
            }

            return res
        }
    }
}