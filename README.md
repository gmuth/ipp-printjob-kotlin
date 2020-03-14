# ipp-printjob-kotlin

A minimal ipp client implementation for jvm written in kotlin.


### Build

In order to build the software you need an installed JDK.

    ./gradlew

When the build has finished you can find `printjob.jar` in `build/libs`

### Distribution

If you don't want to build the software [binary releases](https://github.com/gmuth/ipp-printjob-kotlin/releases) are provided.

### Usage

The tool takes two arguments: *printer-uri* and *file-name*. \
The url scheme `ipp://` is not supported - use `http://` instead. \
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
