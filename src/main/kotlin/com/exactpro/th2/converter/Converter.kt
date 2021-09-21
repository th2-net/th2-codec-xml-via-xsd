package com.exactpro.th2.converter

import com.exactpro.th2.common.grpc.ListValue
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.message.*
import com.exactpro.th2.common.value.getMessage
import com.exactpro.th2.common.value.toListValue
import com.exactpro.th2.common.value.toValue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.*
import com.google.protobuf.util.JsonFormat
import java.lang.IllegalArgumentException


class Converter {
    companion object {
        private fun jsonToValue(node: JsonNode, name: String = "") : Value {
            when (node) {
                is ObjectNode -> {
                    return Value.newBuilder().apply {
                        messageValueBuilder.apply {
                            node.fieldNames().forEach {
                                putFields(it, jsonToValue(node[it], it))
                            }
                        }
                    }.build()
                }
                is ArrayNode -> {
                    return Value.newBuilder().apply {
                        messageValueBuilder.apply {
                            listValue = listValueBuilder.addAllValues(node.toListValue().valuesList).build()

                            val listValueBuilder = ListValue.newBuilder()

                            node.toMutableList().forEach { listValue = listValueBuilder.addValues(Value.newBuilder().setSimpleValue(it.textValue())).build() }
                        }
                    }.build()
                }
                is ValueNode -> {
                    return Value.newBuilder().apply {
                        messageValueBuilder.apply {
                            this[name] = node.textValue()
                        }
                    }.build()
                }
                else -> error("Unknown node type ${node::class.java}")
            }
        }

        fun convertJsonToProto(node: JsonNode) : Message {
            return jsonToValue(node).getMessage()
                ?: throw IllegalArgumentException("JsonNode $node does not contain a message")
        }

        fun convertProtoToJson(message: Message) : String {
            val mapper = ObjectMapper()
            val node = mapper.createObjectNode()

            node.apply {
                message.allFields.forEach {
                    put(it.key.jsonName, it.value.toString())
                }
            }

            return node.toString()
        }
    }
}