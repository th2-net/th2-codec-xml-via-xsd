package com.exactpro.th2.codec.xml

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
    val attributes: MutableMap<String, String> = mutableMapOf()
    val childNodes: MutableMap<QName, MutableList<NodeContent>> = mutableMapOf()

    var text: String? = null
    var type: Value.KindCase = SIMPLE_VALUE

    fun StreamReaderDelegateDecorator.addAttributes() {
        if (attributeCount > 0) {
            for (i in 0 until attributeCount) {
                attributes[getAttributeName(i).localPart] = getAttributeValue(i)
            }
        }
    }

    fun setMessageType() {
        if (this.type == SIMPLE_VALUE) {
            this.type = MESSAGE_VALUE
        }
    }

    fun release(messageBuilder: Message.Builder) {

        // TODO: don't forget about attributes

        childNodes.forEach {
            messageBuilder.addNode(it.key, it.value)
        }

        messageBuilder.addField(nodeName.localPart, this)
    }

    private fun Message.Builder.addNode(nodeName: QName, nodeList: MutableList<NodeContent>) {
        println("Message addNode $nodeName")
        nodeList.forEach { node ->
            when (node.type) {
                MESSAGE_VALUE -> {
                    val count = nodeList.count()
                    if (count > 1) {
                        val list = listValue()

                        nodeList.forEach { nodeContent ->
                            nodeContent.childNodes.forEach {
                                list.addNode(it.key, it.value)
                            }
                        }

                        addField(nodeName.localPart, list)
                    } else if (count == 1) {
                        val message = message()

                        node.childNodes.forEach {
                            message.addNode(it.key, it.value)
                        }

                        addField(nodeName.localPart, message)
                    }
                }

                // TODO: mb I it's possible to have a list of simple values too
                SIMPLE_VALUE -> addField(node.nodeName.localPart, node.text)

                else -> throw IllegalArgumentException("Node $node can be either MESSAGE_VALUE or SIMPLE_VALUE")
            }
        }
    }

    private fun ListValue.Builder.addNode(nodeName: QName, nodeList: MutableList<NodeContent>) {
        println("List addNode $nodeName, nodeList $nodeList")
        nodeList.forEach { node ->
            when (node.type) {
                MESSAGE_VALUE -> {
                    val count = nodeList.count()
                    if (count > 1) {
                        val list = listValue()

                        node.childNodes.forEach {
                            list.addNode(it.key, it.value)
                        }

                        add(list)
                    } else if (count == 1) {
                        val message = message()

                        node.childNodes.forEach {
                            message.addNode(it.key, it.value)
                        }

                        add(message)
                    }
                }

                // TODO: mb I it's possible to have a list of simple values too
                SIMPLE_VALUE -> add(node.text)

                else -> throw IllegalArgumentException("Node $node can be either MESSAGE_VALUE or SIMPLE_VALUE")
            }
        }
    }

    override fun toString(): String {
        return "NodeContent(attributes=$attributes, childNodes=$childNodes, text=$text, type=$type)"
    }
}