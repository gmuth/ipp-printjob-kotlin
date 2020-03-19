package ipp

// --------------------
// Author: Gerhard Muth
// Date  : 19.3.2020
// --------------------

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI

fun printJobWithoutResponseDecoding(uri: URI, documentInputStream: InputStream) {
    val charset = Charsets.UTF_8
    with(uri.toURL().openConnection() as HttpURLConnection) {
        setDoOutput(true)
        setRequestProperty("Content-Type", "application/ipp")
        // encode ipp request 'Print-Job operation'
        with(DataOutputStream(outputStream)) {
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
            // append document
            documentInputStream.copyTo(outputStream)
            close()
            outputStream.close()
        }
        // get http status and ipp status
        if (responseCode == 200) {
            val dataInputStream = DataInputStream(inputStream)
            println(String.format("ipp version %d.%s", dataInputStream.readByte(), dataInputStream.readByte()))
            println(String.format("ipp status: %04X", dataInputStream.readShort()))
        } else {
            throw IOException("post to $uri failed with http status $responseCode")
        }
    }
}