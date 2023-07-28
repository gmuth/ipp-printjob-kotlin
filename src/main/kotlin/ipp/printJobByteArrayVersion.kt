package ipp

// --------------------
// Author: Gerhard Muth
// Date  : 15.3.2020
// --------------------

import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.Charset

fun main(args: Array<String>) {
    val (printerURI, documentInputStream) = getArgsOrThrowUsage(args)
    printJobByteArrayVersion(printerURI, documentInputStream)
}

fun printJobByteArrayVersion(uri: URI, documentInputStream: InputStream) {

    // encode Print-Job operation
    val byteArrayOutputStream = ByteArrayOutputStream()
    val ippRequest = with(DataOutputStream(byteArrayOutputStream)) {
        fun writeAttribute(tag: Int, name: String, value: String) {
            writeByte(tag)
            writeShort(name.length)
            write(name.toByteArray(Charsets.US_ASCII))
            writeShort(value.length)
            write(value.toByteArray(Charsets.US_ASCII))
        }
        writeShort(0x0101) // ipp version
        writeShort(0x0002) // print job operation
        writeInt(0x002A) // request id
        writeByte(0x01) // operation group tag
        writeAttribute(0x47, "attributes-charset", "us-ascii")
        writeAttribute(0x48, "attributes-natural-language", "en")
        writeAttribute(0x45, "printer-uri", "$uri")
        writeByte(0x03) // end tag
        byteArrayOutputStream.toByteArray()
    }

    // exchange ipp messages via http
    println("send ipp request to $uri")
    val httpScheme = uri.scheme.replace("ipp", "http")
    val httpUri = URI.create("$httpScheme:${uri.rawSchemeSpecificPart}")
    val ippResponse = with(httpUri.toURL().openConnection() as HttpURLConnection) {
        val ippContentType = "application/ipp"
        connectTimeout = 3000
        doOutput = true
        setRequestProperty("Content-Type", ippContentType)
        SequenceInputStream(ippRequest.inputStream(), documentInputStream).copyTo(outputStream)
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
        fun readString(charset: Charset = Charsets.US_ASCII) =
            String(ByteArray(readShort().toInt()).also { read(it) }, charset)
        println(String.format("version %d.%d", readByte(), readByte()))
        println(String.format("status  %d", readShort()))
        println(String.format("request %d", readInt()))
        do {
            val tag = readByte()
            if (tag < 0x10) { // delimiter
                println(String.format("group %02X", tag))
            } else { // attribute value
                val name = readString()
                val value: Any = when (tag.toInt()) {
                    0x21, 0x23 -> {
                        readShort()
                        readInt()
                    }
                    0x41, 0x44, 0x45, 0x47, 0x48 -> {
                        readString(Charsets.UTF_8)
                    }
                    else -> {
                        readString()
                        String.format("<decoding-tag-%02X-not-implemented>", tag)
                    }
                }
                println(String.format("   %s (%02X) = %s", name, tag, value))
            }
        } while (tag != 0x03.toByte())
    }
}