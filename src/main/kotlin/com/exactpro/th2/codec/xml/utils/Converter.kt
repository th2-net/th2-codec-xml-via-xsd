/*
 * Copyright 2021-2023 Exactpro (Exactpro Systems Limited)
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
import com.exactpro.th2.common.grpc.Message as ProtoMessage
import com.exactpro.th2.common.grpc.RawMessage as ProtoRawMessage
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.message.messageType
import com.exactpro.th2.common.message.set
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.RawMessage
import com.exactpro.th2.common.value.getMessage
import com.exactpro.th2.common.value.toValue
import java.lang.IllegalArgumentException

@Suppress("UNCHECKED_CAST")
private fun MutableMap<String, *>.toProtoValue(name: String = ""): Value {
    this.removeSelfClosing()
    if (this.isEmpty()) {
        return Message.getDefaultInstance().toValue()
    }
    val message = message().also { builder ->
        builder.messageType = name
    }
    for ((key, value) in this) {
        message[key] = when (value) {
            is Map<*, *> -> (value as MutableMap<String, *>).toProtoValue()
            is String -> value
            is List<*> -> value.mapNotNull {
                when (it) {
                    is Map<*, *> -> (it as MutableMap<String, *>).toProtoValue()
                    else -> it.toValue()
                }
            }
            null -> continue
            else -> error("Unsupported type of value: ${value::class.simpleName}")
        }
    }
    return message.build().toValue()
}

fun MutableMap<String, *>.toProto(type: String, rawMessage: ProtoRawMessage): ProtoMessage {
    val builder = toProtoValue(type).getMessage()?.toBuilder()
        ?: throw IllegalArgumentException("JsonNode $this does not contain a message")
    val rawMetadata = rawMessage.metadata

    if (rawMessage.hasParentEventId()) builder.parentEventId = rawMessage.parentEventId

    builder.metadataBuilder.apply {
        id = rawMetadata.id
        protocol = XmlPipelineCodecFactory.PROTOCOL
        putAllProperties(rawMetadata.propertiesMap)
    }

    return builder.build()
}

fun Map<String, *>.toTransport(type: String, rawMessage: RawMessage): ParsedMessage {
    return ParsedMessage(
        id = rawMessage.id,
        eventId = rawMessage.eventId,
        type = type,
        metadata = rawMessage.metadata,
        protocol = XmlPipelineCodecFactory.PROTOCOL,
        body = this
    )
}

fun MutableMap<String, *>.removeSelfClosing() = this.apply { remove("-self-closing") }