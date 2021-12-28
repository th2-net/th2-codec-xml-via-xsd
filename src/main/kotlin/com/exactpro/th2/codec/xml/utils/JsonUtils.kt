/*
 * Copyright 2021-2022 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.codec.xml.utils

import com.exactpro.th2.common.grpc.ListValue
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Value
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

private fun ObjectNode.putObject(name: String, message: Message): Unit = putObject(name).let { newMessageNode ->
    message.fieldsMap.forEach {
        newMessageNode.putField(it.key, it.value)
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