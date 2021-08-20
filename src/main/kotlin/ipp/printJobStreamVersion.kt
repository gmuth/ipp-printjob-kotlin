package ipp

// --------------------
// Author: Gerhard Muth
// Date  : 15.3.2020
// --------------------

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.Charset

fun main(args: Array<String>) {
    val (printerURI, documentInputStream) = getArgsOrThrowUsage(args)
    printJobStreamVersion(printerURI, documentInputStream)
}

fun printJobStreamVersion(uri: URI, documentInputStream: InputStream) {
    var charset = Charsets.UTF_8
    val ippContentType = "application/ipp"

    println("send ipp request to $uri")
    val httpScheme = uri.scheme.replace("ipp", "http")
    val httpUri = URI.create("${httpScheme}:${uri.schemeSpecificPart}")
    val httpURLConnection = httpUri.toURL().openConnection() as HttpURLConnection
    with(httpURLConnection) {
        setConnectTimeout(3000)
        setDoOutput(true)
        setRequestProperty("Content-Type", ippContentType)
    }
    // encode ipp request 'Print-Job operation'
    with(DataOutputStream(httpURLConnection.outputStream)) {
        fun writeAttribute(tag: Int, name: String, value: String) {
            writeByte(tag)
            writeShort(name.length)
            write(name.toByteArray(Charsets.US_ASCII))
            writeShort(value.length)
            write(value.toByteArray(charset))
        }
        writeShort(0x0101) // ipp version 1.1
        writeShort(0x0002) // print job operation
        writeInt(1) // request 1
        writeByte(0x01) // operation group tag
        writeAttribute(0x47, "attributes-charset", charset.name().toLowerCase())
        writeAttribute(0x48, "attributes-natural-language", "en")
        writeAttribute(0x45, "printer-uri", "$uri")
        writeByte(0x03) // end tag
        documentInputStream.copyTo(this) // append document
    }
    // check http response
    with(httpURLConnection) {
        if (getHeaderField("Content-Type") != ippContentType) {
            throw IOException("response from $uri is not '$ippContentType'")
        }
        if (responseCode != 200) {
            throw IOException("post to $uri failed with http status $responseCode")
        }
    }
    // decode ipp response
    with(DataInputStream(httpURLConnection.inputStream)) {
        fun readValue() = ByteArray(readShort().toInt()).also { read(it) }
        println(String.format("version %d.%d", readByte(), readByte()))
        println(String.format("status  %d", readShort()))
        println(String.format("request %d", readInt()))
        do {
            val tag = readByte()
            if (tag < 0x10) { // delimiter
                println(String.format("group %02X", tag))
            } else { // attribute with value(s)
                val name = String(readValue(), Charsets.US_ASCII)
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
                if (name == "attributes-charset") charset = Charset.forName(value as String)
            }
        } while (tag != 0x03.toByte())
    }
}