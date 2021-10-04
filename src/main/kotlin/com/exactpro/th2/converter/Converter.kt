package com.exactpro.th2.converter

import com.exactpro.th2.codec.xml.XmlPipelineCodecFactory
import com.exactpro.th2.common.grpc.ListValue
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.message.*
import com.exactpro.th2.common.value.getMessage
import com.exactpro.th2.common.value.toListValue
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
                                putFields(it, jsonToValue(node[it], it))
                            }
                        }
                    }.build()
                }
                is ArrayNode -> {
                    return Value.newBuilder().apply {
                        messageValueBuilder.apply {
                            messageType = name
                            listValue = listValueBuilder.addAllValues(node.toListValue().valuesList).build()

                            val listValueBuilder = ListValue.newBuilder()

                            node.toMutableList().forEach {
                                val textValue = it.textValue()

                                if (textValue != null) {
                                    listValue =
                                        listValueBuilder.
                                        addValues(Value.newBuilder()
                                            .setSimpleValue(textValue)).build()
                                }
                            }
                        }
                    }.build()
                }
                is ValueNode -> {
                    return Value.newBuilder().apply {
                        messageValueBuilder.apply {
                            messageType = name
                            this[name] = node.textValue()
                        }
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