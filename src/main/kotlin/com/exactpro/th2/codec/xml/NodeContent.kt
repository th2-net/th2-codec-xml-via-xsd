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
            for (i in 0 until decorator.attributeCount) {
                val localPart = decorator.getAttributeName(i).localPart

                attributes[localPart] = decorator.getAttributeValue(i)
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

                            list.addNode(subMessage, it.key, it.value)
                            list.add(subMessage)
                            addField(nodeName.localPart, list)
                        }

                    } else if (count == 1) {
                        val message = message()
                        message.writeAttributes(node)

                        node.childNodes.forEach {
                            message.addNode(it.key, it.value)
                        }

                        addField(nodeName.localPart, message)
                    }
                }

                // TODO: mb I it's possible to have a list of simple values too
                SIMPLE_VALUE -> {
                    writeAttributes(node)
                    val text = node.textSB.toString()

                    if (text.isNotBlank()) {
                        addField(node.nodeName.localPart, text)
                    }
                }

                else -> throw IllegalArgumentException("Node $node can be either MESSAGE_VALUE or SIMPLE_VALUE")
            }
        }
    }

    private fun ListValue.Builder.addNode(messageBuilder: Message.Builder, nodeName: QName, nodeList: MutableList<NodeContent>) {
        val count = nodeList.count()

        val message = message()

        nodeList.forEach { node ->
//            val message = message()

            message.writeAttributes(node)

            when (node.type) {
                MESSAGE_VALUE -> {

                    if (count > 1) {
                        val list = listValue()

                        node.childNodes.forEach {
                            message.addNode(it.key, it.value)
                            messageBuilder.addField(it.key.localPart, message)
                        }

                        add(list)
                    } else if (count == 1) {
                        node.childNodes.forEach {
                            val subMessage = message()
                            subMessage.addNode(it.key, it.value)
                            message.addField(it.key.localPart, subMessage)
                        }

                        messageBuilder.addField(node.nodeName.localPart, message)
                    }
                }

                // TODO: mb I it's possible to have a list of simple values too
                SIMPLE_VALUE -> {
                    val subMessage = message()
                    subMessage.writeAttributes(node)
                    messageBuilder.addField(node.nodeName.localPart, subMessage)

                    val text = node.textSB.toString()

                    if (text.isNotBlank()) {
                        messageBuilder.addField(nodeName.localPart, text)
                    }
                }

                else -> throw IllegalArgumentException("Node $node can be either MESSAGE_VALUE or SIMPLE_VALUE")
            }
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