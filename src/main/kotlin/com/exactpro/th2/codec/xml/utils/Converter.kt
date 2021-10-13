/*
 * Copyright 2021-2021 Exactpro (Exactpro Systems Limited)
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
import com.exactpro.th2.common.grpc.ListValue
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.message.*
import com.exactpro.th2.common.value.getMessage
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ValueNode
import java.lang.IllegalArgumentException

class Converter {
    companion object {
        private fun jsonToValue(node: JsonNode, name: String = "") : Value {
            when (node) {
                is ObjectNode -> {
                    return Value.newBuilder().apply {
                        messageValueBuilder.apply {
                            messageType = name
                            node.fieldNames().forEach {
                                putFields(it, jsonToValue(node[it]))
                            }
                        }
                    }.build()
                }
                is ArrayNode -> {
                    return Value.newBuilder().apply {
                        messageValueBuilder.apply {
//                            messageType = name
//                            listValue = listValueBuilder.addAllValues(node.toListValue().valuesList).build()
                            val listValueBuilder = ListValue.newBuilder()
                            node.forEach {
                                listValueBuilder.addValues(jsonToValue(it))
                            }
                            listValue = listValueBuilder.build()
                        }
                    }.build()
                }
                is ValueNode -> {
                    return Value.newBuilder().apply {
                        simpleValue = node.textValue()
                    }.build()
                }
                else -> error("Unknown node type ${node::class.java}")
            }
        }

        fun convertJsonToProto(node: JsonNode, type: String, rawMessage: RawMessage) : Message = jsonToValue(node, type).getMessage()!!
            .toBuilder().apply {
                parentEventId = rawMessage.parentEventId
                metadataBuilder.also { msgMetadata ->
                    rawMessage.metadata.also { rawMetadata ->
                        msgMetadata.id = rawMetadata.id
                        msgMetadata.timestamp = rawMetadata.timestamp
                        msgMetadata.protocol = XmlPipelineCodecFactory.PROTOCOL
                        msgMetadata.putAllProperties(rawMetadata.propertiesMap)
                    }
                }
            }.build()
            ?: throw IllegalArgumentException("JsonNode $node does not contain a message")

        fun convertProtoToJson(message: Message) : String = ObjectMapper().createObjectNode().apply {
            message.fieldsMap.forEach {
                putField(it.key, it.value)
            }
        }.toString()




    }
}