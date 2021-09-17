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
package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.utils.XmlTest
import com.exactpro.th2.codec.xml.utils.parsedMessage
import com.exactpro.th2.common.message.addFields
import org.junit.jupiter.api.Test

class XmlPipelineCodecTest : XmlTest() {

    @Test
    fun `test common`() {
        val xml = """
            <CommonFieldsA>
                <f>123</f>
                <abc>
                    <ab>
                        <a>345</a>
                        <b>678</b>
                    </ab>
                    <c>90</c>
                </abc>
            </CommonFieldsA>
        """

        val json = """{
  "CommonFieldsA": {
    "f": "123",
    "abc": {
      "ab": {
        "a": "345",
        "b": "678"
      },
      "c": "90"
    }
  }
}"""
        val msg = parsedMessage("CommonFieldsA").addFields(
            "json", json,
        )
        checkDecode(xml, msg)
    }

}