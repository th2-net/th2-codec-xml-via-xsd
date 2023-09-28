/*
 * Copyright 2022 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.api.IPipelineCodec
import com.exactpro.th2.codec.api.impl.ReportingContext
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.ByteArrayInputStream
import kotlin.io.path.Path
import kotlin.io.path.readBytes

class XmlDecodeTest {

    companion object {
        private val XML_FILE_PATH = Path("tmp", "test.xml")
        private val PROTO_FILE_PATH = Path("tmp", "test_orig.bin")
        private const val ITERATIONS = 10_000
        private const val CYCLES = 5

        @JvmStatic
        fun main(args: Array<String>) {
            val xml = XML_FILE_PATH.readBytes()
            val proto = PROTO_FILE_PATH.readBytes()
            val message = AnyMessage.newBuilder().setRawMessage(RawMessage.newBuilder().apply {
                metadataBuilder.apply {
                    protocol = XmlPipelineCodecFactory.PROTOCOL
                    idBuilder.connectionIdBuilder.sessionAlias = "test_session_alias"
                }
                body = ByteString.copyFrom(xml)
            })
            val messageGroup = MessageGroup.newBuilder().apply {
                addMessages(message)
            }.build()

            val protoMessageGroup: MessageGroup = MessageGroup.parseFrom(proto)

            XmlPipelineCodecFactory().apply {
                init(ByteArrayInputStream(byteArrayOf()))
            }.use { factory ->
                val codec = factory.create()

                for (cycle in 0 until CYCLES) {
                    parse(codec, messageGroup)
                }

                parse(codec, messageGroup).also {
                    with(JsonFormat.printer()) {
                        assertEquals(print(protoMessageGroup), print(it))
                    }
                }
            }
        }

        private fun parse(
            codec: IPipelineCodec,
            messageGroup: MessageGroup
        ): MessageGroup {
            var millisecond = 0L
            lateinit var result: MessageGroup

            for (i in 0..ITERATIONS) {
                val start = System.currentTimeMillis()
                result = codec.decode(messageGroup, ReportingContext())
                millisecond += System.currentTimeMillis() - start
            }
            println("Avg ${ITERATIONS.toDouble() / (millisecond) * 1_000}")
            return result
        }
    }
}