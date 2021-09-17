package com.exactpro.th2.codec.xml.utils

import com.exactpro.th2.common.grpc.AnyMessage
import com.exactpro.th2.common.grpc.Direction
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageGroup
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.grpc.Value
import com.exactpro.th2.common.message.message
import com.exactpro.th2.common.value.get
import com.google.protobuf.ByteString
import org.opentest4j.AssertionFailedError
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

fun createRawMessage(xml: String): MessageGroup = MessageGroup
    .newBuilder()
    .addMessages(
        AnyMessage
        .newBuilder()
        .setRawMessage(
            RawMessage
            .newBuilder().apply {
                metadataBuilder.protocol = "XML"
                metadataBuilder.idBuilder.connectionIdBuilder.sessionAlias = "test_session_alias"
                body = ByteString.copyFromUtf8("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n$xml")
            }
        )
    )
    .build()

fun assertEqualsMessages(expected: Message, actual: Message, checkMetadata: Boolean = false) {
    if (checkMetadata) {
        assertEquals(expected.metadata.messageType, actual.metadata.messageType, "Not equals message types")
        assertEquals(expected.metadata.protocol, actual.metadata.protocol, "Not equals protocols")
        assertEquals(expected.metadata.id, actual.metadata.id, "Not equals ids")
    }

    assertEquals(
        expected.fieldsCount, actual.fieldsCount,
        "Wrong count fields in actual message"
    )

    expected.fieldsMap.forEach { (fieldName, expectedValue) ->
        val actualValue = actual.fieldsMap[fieldName]
        assertNotNull(actualValue, "Field with name '$fieldName' shouldn't be null")
        try {
            assertEqualsValue(expectedValue, actualValue, checkMetadata)
        } catch (e: AssertionFailedError) {
            throw AssertionFailedError(
                "Error in field with name '$fieldName'.\n${e.message}",
                e.expected,
                e.actual,
                e.cause
            )
        }
    }
}

fun assertEqualsValue(expected: Value, actual: Value, checkMetadata: Boolean = false) {
    assertEquals(expected.kindCase, actual.kindCase, "Different value types")

    when (actual.kindCase) {
        Value.KindCase.SIMPLE_VALUE -> assertEquals(expected.simpleValue, actual.simpleValue)
        Value.KindCase.LIST_VALUE -> {
            assertEquals(
                expected.listValue.valuesCount,
                actual.listValue.valuesCount,
                "Wrong count of element in value"
            )
            expected.listValue.valuesList?.forEachIndexed { i, it -> assertEqualsValue(it, actual.listValue[i]) }
        }
        Value.KindCase.MESSAGE_VALUE ->
            assertEqualsMessages(expected.messageValue, actual.messageValue, checkMetadata)
        else -> error("Unknown value type")
    }
}

fun parsedMessage(messageType: String): Message.Builder = message(messageType).apply {
    metadataBuilder.apply {
        protocol = "XML"
        idBuilder.apply {
            direction = Direction.FIRST
            connectionIdBuilder.sessionAlias = "test_session_alias"
        }
    }
}