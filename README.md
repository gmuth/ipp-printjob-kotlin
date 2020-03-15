# ipp-printjob-kotlin

A minimal client implementation of the ipp-protocol written in kotlin for the jvm.

### Build

In order to build the software you need an installed JDK.

    ./gradlew

When the build has finished you can find `printjob.jar` in `build/libs`

### Distribution

For the impatient [binary releases](https://github.com/gmuth/ipp-printjob-kotlin/releases) are provided. 
Directory `demo` contains the `printjob.jar` and a test script called `go` that submits a blank PDF to Apple's Printer Simulator.
Registered Apple developers can download
[Additional Tools for Xcode](https://download.developer.apple.com/Developer_Tools/Additional_Tools_for_Xcode_11/Additional_Tools_for_Xcode_11.dmg)
containing the Printer Simulator.

### Usage

The tool takes two arguments: *printer-uri* and *file-name*. 
The url scheme `ipp://` is not supported - use `http://` instead.
If you don't know the printer uri try `ippfind`. 

    java -jar printjob.jar http://colorjet:631/ipp/printer A4-blank.pdf
    
    send ipp request to http://colorjet:631/ipp/printer
    ipp response status: 0000
    group 01
       attributes-charset (47) = utf-8
       attributes-natural-language (48) = en
    group 02
       job-uri (45) = ipp://colorjet:631/jobs/352
       job-id (21) = 352
       job-state (23) = 3
       job-state-reasons (44) = none
    group 03
    
### Document Format

The operation attributes group does not include a value for `document-format`.
This should be equivalent to `application/octet-stream` indicating the printer has to auto sense the document format.
You have to make sure the printer supports the document format you send - PDF is usually a good option.