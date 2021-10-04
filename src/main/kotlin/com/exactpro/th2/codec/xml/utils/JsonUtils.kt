package com.exactpro.th2.codec.xml.utils

import com.exactpro.th2.common.grpc.ListValue
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Value
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

private fun ObjectNode.putObject(name: String, message: Message) {
    putObject(name).let { newMessageNode ->
        message.fieldsMap.forEach {
            newMessageNode.putField(it.key, it.value)
        }
    }

}

fun ObjectNode.putField(name: String, field: Value) {
    if (field.hasMessageValue()) {
        putObject(name, field.messageValue)
    } else if (field.hasListValue()) {
        putArray(name, field.listValue)
    } else {
        put(name, field.simpleValue)
    }
}

private fun ObjectNode.putArray(name: String, listValue: ListValue) {
    putArray(name).let { arrayNode ->
        listValue.valuesList.forEach {
            arrayNode.putField(it)
        }
    }
}

private fun ArrayNode.putField(field: Value) {
    if (field.hasMessageValue()) {
        addObject(field.messageValue)
    } else {
        add(field.simpleValue)
    }
}

private fun ArrayNode.addObject(message: Message) {
    addObject().let { newMessageNode ->
        message.fieldsMap.forEach {
            newMessageNode.putField(it.key, it.value)
        }
    }
}