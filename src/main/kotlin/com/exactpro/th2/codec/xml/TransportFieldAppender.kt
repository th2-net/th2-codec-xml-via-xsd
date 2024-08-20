/*
 * Copyright 2023 Exactpro (Exactpro Systems Limited)
 *
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

object TransportFieldAppender : FieldAppender<MutableMap<String, Any>> {
    override fun MutableMap<String, Any>.appendSimple(name: FieldName, value: String) {
        put(name, value)
    }

    override fun MutableMap<String, Any>.appendNode(name: FieldName, node: NodeContent<MutableMap<String, Any>>) {
        put(name, node.extractValue())
    }

    override fun MutableMap<String, Any>.appendNodeCollection(
        name: FieldName,
        nodes: List<NodeContent<MutableMap<String, Any>>>,
    ) {
        put(name, nodes.asSequence().map { it.extractValue() }.toList())
    }

    private fun NodeContent<MutableMap<String, Any>>.extractValue(): Any =
        if (isMessage) {
            toMessage()
        } else {
            toText()
        }
}