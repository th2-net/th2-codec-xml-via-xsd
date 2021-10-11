package com.exactpro.th2.codec.xml.utils


import org.w3c.dom.ls.LSInput
import org.w3c.dom.ls.LSResourceResolver
import java.io.*
import java.util.*

class ResourceResolver(private val xsdInputs: ArrayList<Pair<String, ByteArray>>) : LSResourceResolver {

    override fun resolveResource(
        type: String?,
        namespaceURI: String,
        publicId: String?,
        systemId: String?,
        baseURI: String?
    ): LSInput? {

        xsdInputs.forEach {
            if (it.first == namespaceURI){
                return Input(it.second)
            }
        }
        return null

        // note: in this sample, the XSD's are expected to be in the root of the classpath
        // val resourceAsStream = this.javaClass.classLoader.getResourceAsStream(xsds[namespaceURI]?)
    }

}

class Input(private val byteArray: ByteArray) : LSInput {

    override fun setSystemId(systemId: String) {

    }

    override fun getPublicId(): String? {
        return null
    }
    override fun getBaseURI(): String? {
        return null
    }

    override fun getByteStream(): InputStream {
        return ByteArrayInputStream(byteArray)
    }

    override fun getCertifiedText(): Boolean {
        return false
    }

    override fun getCharacterStream(): Reader? {
        return null
    }

    override fun getEncoding(): String? {
        return null
    }

    override fun getStringData(): String? {
        val s = Scanner(byteStream).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
    }

    override fun setBaseURI(baseURI: String) {}
    override fun setByteStream(byteStream: InputStream) {}
    override fun setCertifiedText(certifiedText: Boolean) {}
    override fun setCharacterStream(characterStream: Reader?) {}
    override fun setEncoding(encoding: String) {}
    override fun setStringData(stringData: String) {}
    override fun getSystemId(): String? {
        return null
    }


    override fun setPublicId(publicId: String?) {

    }
}