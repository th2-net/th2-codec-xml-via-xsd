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

import com.exactpro.th2.codec.xml.XmlPipelineCodecFactory
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.message.addField
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.messageType
import com.exactpro.th2.common.value.getMessage
import com.exactpro.th2.common.value.toValue
import java.lang.IllegalArgumentException

@Suppress("UNCHECKED_CAST")
private fun Map<String, *>.toProtoValue(name: String = ""): Value {
    val message = message().also { builder ->
        builder.messageType = name
    }
    this.forEach {
        when(it.value) {
            is Map<*,*> -> {
                message.addField(it.key, (it.value as Map<String, *>).toProtoValue())
            }
            is String -> {
                message.addField(it.key, it.value)
            }
            is ArrayList<*> -> {
                when((it.value as ArrayList<*>)[0]) {
                    is Map<*,*> -> message.addField(it.key, (it.value as ArrayList<*>).map { arrayElement -> (arrayElement as Map<String,String>).toProtoValue() }.toValue())
                    else -> message.addField(it.key, (it.value as ArrayList<*>).toValue())
                }
            }
            null -> Unit
            else -> {
                error("Unsupported type of value: ${it.value!!::class.simpleName}")
            }
        }
    }
    return message.build().toValue()
}

fun Map<String, *>.toProto(type: String, rawMessage: RawMessage): Message = this.toProtoValue(type).getMessage()!!.toBuilder().also { builder ->
    builder.parentEventId = rawMessage.parentEventId
    builder.metadataBuilder.also { msgMetadata ->
        rawMessage.metadata.also { rawMetadata ->
            msgMetadata.id = rawMetadata.id
            msgMetadata.timestamp = rawMetadata.timestamp
            msgMetadata.protocol = XmlPipelineCodecFactory.PROTOCOL
            msgMetadata.putAllProperties(rawMetadata.propertiesMap)
        }
    }
}.build() ?: throw IllegalArgumentException("JsonNode $this does not contain a message")
