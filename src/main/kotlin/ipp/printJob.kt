package ipp

// --------------------
// Author: Gerhard Muth
// Date  : 15.3.2020
// --------------------

import java.io.File
import java.io.FileInputStream
import java.net.URI

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("usage: java -jar printjob.jar <printer-uri> <file>")
        return
    }
    val printerURI = URI.create(args[0])
    val file = File(args[1])
    printjobStreamingversion(printerURI, FileInputStream(file))
}