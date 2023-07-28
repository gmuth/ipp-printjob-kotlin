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

fun main(args: Array<String>) {
    val (printerURI, documentInputStream) = getArgsOrThrowUsage(args)
    printJobWithStatusOnly(printerURI, documentInputStream)
}

fun printJobWithStatusOnly(uri: URI, documentInputStream: InputStream) {
    val httpScheme = uri.scheme.replace("ipp", "http")
    val httpUri = URI.create("$httpScheme:${uri.rawSchemeSpecificPart}")
    with(httpUri.toURL().openConnection() as HttpURLConnection) {
        connectTimeout = 3000
        doOutput = true
        setRequestProperty("Content-Type", "application/ipp")
        // encode ipp request 'Print-Job operation'
        with(DataOutputStream(outputStream)) {
            fun writeAttribute(tag: Int, name: String, value: String) {
                writeByte(tag)
                writeShort(name.length)
                write(name.toByteArray(Charsets.US_ASCII))
                writeShort(value.length)
                write(value.toByteArray(Charsets.US_ASCII))
            }
            writeShort(0x0101) // ipp version 1.1
            writeShort(0x0002) // print job operation
            writeInt(1) // request 1
            writeByte(0x01) // operation group tag
            writeAttribute(0x47, "attributes-charset", "us-ascii")
            writeAttribute(0x48, "attributes-natural-language", "en")
            writeAttribute(0x45, "printer-uri", "$uri")
            writeByte(0x03) // end tag
            // append document
            documentInputStream.copyTo(this)
            close()
        }
        // get http status and ipp status
        if (responseCode == 200) DataInputStream(inputStream).run {
            println(String.format("version %d.%d", readByte(), readByte()))
            println(String.format("status  %04X", readShort())) // 0 = SuccessfulOk
        } else {
            throw IOException("post to $uri failed with http status $responseCode")
        }
    }
}