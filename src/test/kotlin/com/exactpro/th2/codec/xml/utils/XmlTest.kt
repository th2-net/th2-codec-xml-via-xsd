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

import com.exactpro.th2.codec.api.IPipelineCodec
import com.exactpro.th2.codec.xml.XmlPipelineCodec
import com.exactpro.th2.codec.xml.XmlPipelineCodecSettings
import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageGroup
import com.google.protobuf.TextFormat
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import java.io.File
import java.util.*
import kotlin.test.assertEquals

abstract class XmlTest(pathToType: String? = null) {

    protected val codec: IPipelineCodec

    protected fun checkEncode(xml: String, message: Message.Builder) {
        val group = codec.encode(MessageGroup.newBuilder().addMessages(AnyMessage.newBuilder().setMessage(message)).build())
        assertEquals(1, group.messagesCount)

        LOGGER.info("ENCODE_RESULT: ${TextFormat.shortDebugString(group)}")

        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n$xml",
            group.messagesList[0].rawMessage.body.toStringUtf8()
        )
    }

    protected fun checkDecode(xml: String, message: Message.Builder) {
        val group = codec.decode(createRawMessage(xml))
        assertEquals(1, group.messagesCount)

        LOGGER.info("DECODE_RESULT: ${TextFormat.shortDebugString(group)}")

        assertEqualsMessages(message.build(), group.messagesList[0].message, true)
    }

    init {
        codec = XmlPipelineCodec(XmlPipelineCodecSettings(pathToType))
    }

    protected fun encodeFileToBase64Binary(fileName: String): ByteArray {
        val file = File(fileName)
        return Base64.getEncoder().encode(FileUtils.readFileToByteArray(file))
    }

    companion object {
        private val LOGGER: Logger = KotlinLogging.logger { }
    }
}