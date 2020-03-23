package ipp

// --------------------
// Author: Gerhard Muth
// Date  : 23.3.2020
// --------------------

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI

fun getArgsOrThrowUsage(args: Array<String>): Pair<URI, InputStream> {
    if (args.size == 2)
        return Pair(URI.create(args[0]), FileInputStream(File(args[1])))
    else
        throw IllegalArgumentException("usage: java -jar printjob.jar <printer-uri> <file>")
}