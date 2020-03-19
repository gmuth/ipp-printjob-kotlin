package ipp

// --------------------
// Author: Gerhard Muth
// Date  : 15.3.2020
// --------------------

import java.io.*
import java.net.HttpURLConnection
import java.net.URI

fun printJobByteArrayVersion(uri: URI, documentInputStream: InputStream) {
    val charset = Charsets.UTF_8

    // encode Print-Job operation
    val byteArrayOutputStream = ByteArrayOutputStream()
    val ippRequest = with(DataOutputStream(byteArrayOutputStream)) {
        fun writeAttribute(tag: Int, name: String, value: String) {
            writeByte(tag)
            writeShort(name.length)
            write(name.toByteArray(charset))
            writeShort(value.length)
            write(value.toByteArray(charset))
        }
        writeShort(0x0101) // ipp version
        writeShort(0x0002) // print job operation
        writeInt(0x002A) // request id
        writeByte(0x01) // operation group tag
        writeAttribute(0x47, "attributes-charset", charset.name().toLowerCase())
        writeAttribute(0x48, "attributes-natural-language", "en")
        writeAttribute(0x45, "printer-uri", "$uri")
        writeByte(0x03) // end tag
        close()
        byteArrayOutputStream.close()
        byteArrayOutputStream.toByteArray()
    }

    // exchange ipp messages via http
    println("send ipp request to $uri")
    val ippResponse = with(uri.toURL().openConnection() as HttpURLConnection) {
        val ippContentType = "application/ipp"
        setConnectTimeout(3000)
        setDoOutput(true)
        setRequestProperty("Content-Type", ippContentType)
        val ippRequestInputStream = ippRequest.inputStream()
        SequenceInputStream(ippRequestInputStream, documentInputStream).copyTo(outputStream)
        ippRequestInputStream.close()
        documentInputStream.close()
        outputStream.close()
        // check response
        if (getHeaderField("Content-Type") != ippContentType) {
            throw IOException("response from $uri is not '$ippContentType'")
        }
        if (responseCode != 200) {
            throw IOException("post to $uri failed with http status $responseCode")
        }
        inputStream.readAllBytes()
    }

    // decode ipp response
    with(DataInputStream(ByteArrayInputStream(ippResponse))) {
        fun readValue(): ByteArray = readNBytes(readShort().toInt())
        //readShort() // ignore version
        println(String.format("ipp version %d.%s", readByte(), readByte()))
        println(String.format("ipp status %04X", readShort()))
        readInt() // ignore request id
        var tag: Byte
        do {
            tag = readByte()
            if (tag < 0x10) {
                println(String.format("group %02X", tag))
                continue
            }
            // attribute tag
            val name = String(readValue(), charset)
            val value: Any = when (tag.toInt()) {
                0x21, 0x23 -> {
                    readShort()
                    readInt()
                }
                0x41, 0x44, 0x45, 0x47, 0x48 -> {
                    String(readValue(), charset)
                }
                else -> {
                    readValue()
                    String.format("<decoding-tag-%02X-not-implemented>", tag)
                }
            }
            println(String.format("   %s (%02X) = %s", name, tag, value))
        } while (tag != 0x03.toByte())
    }
}