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

fun Message.toMap(): MutableMap<String, Any> = LinkedHashMap<String, Any>().also { messageMap ->
    this.fieldsMap.forEach {
        messageMap.putField(it.key, it.value)
    }
}

fun MutableMap<String, Any>.putField(name: String, field: Value) {
    when {
        field.hasMessageValue() -> put(name, field.messageValue.toMap())
        field.hasListValue() -> put (name, field.listValue.toArray())
        else -> put(name, field.simpleValue)
    }
}

private fun ListValue.toArray(): ArrayList<*> = ArrayList<Any>().also { resultArray ->
    valuesList.forEach { valueFromList ->
        when {
            valueFromList.hasMessageValue() -> resultArray.add(valueFromList.messageValue.toMap())
            valueFromList.hasListValue() -> resultArray.add(valueFromList.listValue.toArray())
            else -> resultArray.add(valueFromList.simpleValue)
        }
    }
}
