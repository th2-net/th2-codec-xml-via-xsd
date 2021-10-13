package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.xml.xsd.XsdValidator
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

class XsdTest {

    @Test
    fun `test parsing multiple xml, schemas,namespaces`() {
//        val xmlSetPath = ".\\sample-resourses\\XMLset.zip"
//        val xsdSetPath = ".\\sample-resourses\\XSDset.zip"

//        val xsdMap = XmlPipelineCodecFactory.bufferDictionary(Thread.currentThread().contextClassLoader.getResourceAsStream("XSDset.zip")!!)
//        val validator = XsdValidator(xsdMap)
//        val pairList = validator.validate(xmlSetPath, xsdSetPath, bufferPath)
//        pairList.forEach {
//            val message = xmlPipelineCodec.decodePair(it)
//            println("Message :\n" + message.toString())
//            val rawMessage = xmlPipelineCodec.encodeMessage(message)!!
//            assertEquals(it.first.getText(), rawMessage.body.toString(Charsets.UTF_8))
//        }
    }
}