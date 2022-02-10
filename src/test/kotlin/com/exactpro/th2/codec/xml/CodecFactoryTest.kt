package com.exactpro.th2.codec.xml

import com.exactpro.th2.codec.api.IPipelineCodecContext
import com.exactpro.th2.common.schema.dictionary.DictionaryType
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import java.io.File
import java.util.Base64

class CodecFactoryTest {

    @Test
    fun `test init with zip archive inputStream`() {
        val context = mock<IPipelineCodecContext>()
        Mockito.`when`(context.getDictionaryAliases()).thenReturn(emptySet())
        val zipBase64 = encodeFileToBase64Binary(Thread.currentThread().contextClassLoader.getResource("XSDset.zip")!!.path).inputStream()

        Mockito.`when`(context[DictionaryType.MAIN]).thenReturn(zipBase64)

        Assertions.assertDoesNotThrow({ XmlPipelineCodecFactory().init(context) }, "Factory must check non empty zip")

        val emptyZip = encodeFileToBase64Binary(Thread.currentThread().contextClassLoader.getResource("XSDEmptySet.zip")!!.path).inputStream()

        Mockito.`when`(context[DictionaryType.MAIN]).thenReturn(emptyZip)

        Assertions.assertThrows(IllegalArgumentException::class.java, { XmlPipelineCodecFactory().init(context) }, "Factory must check empty zip")
    }

    @Test
    fun `test init with alias file`() {
        val context = mock<IPipelineCodecContext>()

        val aliases = setOf("service", "invoice")
        Mockito.`when`(context.getDictionaryAliases()).thenReturn(aliases)

        for (alias in aliases) {
            Mockito.`when`(context.getFile(alias)).thenReturn(File(Thread.currentThread().contextClassLoader.getResource("aliases/$alias.xsd")!!.toURI()))
        }

        Assertions.assertDoesNotThrow({ XmlPipelineCodecFactory().init(context) }, "Factory must check non empty alias set")
    }


    private fun encodeFileToBase64Binary(path: String): ByteArray {
        val file = File(path)
        return Base64.getEncoder().encode(FileUtils.readFileToByteArray(file))
    }
}