package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.utils.XmlTest
import com.exactpro.th2.codec.xml.utils.parsedMessage
import com.exactpro.th2.common.message.addFields
import org.junit.jupiter.api.Test

class XmlRepeatingTest : XmlTest() {

    @Test
    fun `test simple repeating`() {
        val xml = """
            <SimpleRepeating>
                <user id="1">admin</user>
                <user id="2">user</user>
                <user id="3">guest</user>
            </SimpleRepeating>
        """.trimIndent()

        val json = "{\"user\":[{\"id\":\"1\",\"\":\"admin\"},{\"id\":\"2\",\"\":\"user\"},{\"id\":\"3\",\"\":\"guest\"}]}"
        val msg = parsedMessage("SimpleRepeating").addFields(
            "json", json,
        )
        checkDecode(xml, msg)
    }

    @Test
    fun `test repeating group`() {
        val xml = """
            <RepeatingGroup>
                <groupA>
                    <B>
                        <C>
                            <D>
                                <groupE>
                                    <groupF>
                                        <G>
                                            <H>1</H>
                                        </G>
                                    </groupF>
                                </groupE>
                                <groupE>
                                    <groupF>
                                        <G>
                                            <H>2</H>
                                        </G>
                                    </groupF>
                                    <groupF>
                                        <G>
                                            <H>3</H>
                                        </G>
                                    </groupF>
                                </groupE>
                            </D>
                        </C>
                    </B>
                </groupA>
                <groupA>
                    <B>
                        <C>
                            <D>
                                <groupE>
                                    <groupF>
                                        <G>
                                            <H>4</H>
                                        </G>
                                    </groupF>
                                </groupE>
                                <groupE>
                                    <groupF>
                                        <G>
                                            <H>5</H>
                                        </G>
                                    </groupF>
                                </groupE>
                                <groupE>
                                    <groupF>
                                        <G>
                                            <H>6</H>
                                        </G>
                                    </groupF>
                                    <groupF>
                                        <G>
                                            <H>7</H>
                                        </G>
                                    </groupF>
                                    <groupF>
                                        <G>
                                            <H>8</H>
                                        </G>
                                    </groupF>
                                </groupE>
                            </D>
                        </C>
                    </B>
                </groupA>
            </RepeatingGroup>
        """.trimIndent()

        val json = "{\"groupA\":[{\"B\":{\"C\":{\"D\":{\"groupE\":[{\"groupF\":{\"G\":{\"H\":\"1\"}}},{\"groupF\":[{\"G\":{\"H\":\"2\"}},{\"G\":{\"H\":\"3\"}}]}]}}}},{\"B\":{\"C\":{\"D\":{\"groupE\":[{\"groupF\":{\"G\":{\"H\":\"4\"}}},{\"groupF\":{\"G\":{\"H\":\"5\"}}},{\"groupF\":[{\"G\":{\"H\":\"6\"}},{\"G\":{\"H\":\"7\"}},{\"G\":{\"H\":\"8\"}}]}]}}}}]}"
        val msg = parsedMessage("RepeatingGroup").addFields(
            "json", json,
        )
        checkDecode(xml, msg)
    }
}