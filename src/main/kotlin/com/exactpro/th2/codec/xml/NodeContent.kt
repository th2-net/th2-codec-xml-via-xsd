package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.NodeContent.Companion.writeAttributes
import com.exactpro.th2.common.grpc.ListValue
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.grpc.Value.KindCase.SIMPLE_VALUE
import com.exactpro.th2.common.grpc.Value.KindCase.MESSAGE_VALUE
import com.exactpro.th2.common.grpc.Value.KindCase.LIST_VALUE
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.value.add
import com.exactpro.th2.common.value.listValue
import javax.xml.namespace.QName

class NodeContent(val nodeName: QName) {
    private val attributes: MutableMap<String, String> = mutableMapOf()
    val childNodes: MutableMap<QName, MutableList<NodeContent>> = mutableMapOf()

    var text: String? = null
    var type: Value.KindCase = SIMPLE_VALUE

    fun addAttributes(decorator: StreamReaderDelegateDecorator) {
        if (decorator.attributeCount > 0) {
            println("Number of attr: ${decorator.attributeCount}")
            for (i in 0 until decorator.attributeCount) {
                val localPart = decorator.getAttributeName(i).localPart

                println("Adding attribute $localPart: ${decorator.getAttributeValue(i)} to $nodeName")

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

        println("Message node $nodeName with children ${nodeList.map { it.nodeName }}")

        nodeList.forEach { node ->
            when (node.type) {
                MESSAGE_VALUE -> {
                    if (count > 1) {
                        val subMessage = message()
                        subMessage.writeAttributes(node) // FIXME: 3 times instead of 1 also in places where i dont need it

                        node.childNodes.forEach {
//                            val subMessage = message()
//                            subMessage.writeAttributes(node) // FIXME: not every time

                            list.addNode(subMessage, it.key, it.value)
                            list.add(subMessage)

                            if (it.key.localPart == "Transforms") {
                                println()
                            }
                        }

                        addField(nodeName.localPart, list)
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
                    if (node.nodeName.localPart == "Transform") {
                        println()
                    }
                    writeAttributes(node)
                    node.text?.let { addField(node.nodeName.localPart, it) }
                }

                else -> throw IllegalArgumentException("Node $node can be either MESSAGE_VALUE or SIMPLE_VALUE")
            }
        }
    }

    private fun ListValue.Builder.addNode(messageBuilder: Message.Builder, nodeName: QName, nodeList: MutableList<NodeContent>) {
        val count = nodeList.count()

        val message = message()

        println("List node $nodeName with children ${nodeList.map { it.nodeName }}")

        // FIXME: 9 Transforms instead of 3

        nodeList.forEach { node ->
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
                        if (node.nodeName.localPart == "Transforms") {
                            println()
                        }
                        node.childNodes.forEach {
                            val subMessage = message()
//                            message.addNode(it.key, it.value) //FIXME: here added attr when its not needed
                            subMessage.addNode(it.key, it.value)
                            message.addField(it.key.localPart, subMessage)
                        }

                        if (node.nodeName.localPart == "Transforms") {
                            println()
                        }

                        messageBuilder.addField(node.nodeName.localPart, message)

                        add(message)
                    }
                }

                // TODO: mb I it's possible to have a list of simple values too
                SIMPLE_VALUE -> {
                    // TODO: add attr?
                    node.text?.let { messageBuilder.addField(nodeName.localPart, it) }
                }

                else -> throw IllegalArgumentException("Node $node can be either MESSAGE_VALUE or SIMPLE_VALUE")
            }
        }
    }

    override fun toString(): String {
        return "NodeContent(nodeName=$nodeName, attributes=$attributes, childNodes=$childNodes, text=$text, type=$type)"
    }

    companion object {
        private fun Message.Builder.writeAttributes(nodeContent: NodeContent) {
            nodeContent.attributes.forEach {
                println("Writing attribute ${it.key}: ${it.value} to ${nodeContent.nodeName}")

                if (it.key == "URI") {
                    println()
                }

                if (!containsFields(it.key)) { // FIXME: remove the condition mb
                    addField(it.key, it.value)
                }
            }
        }
    }
}