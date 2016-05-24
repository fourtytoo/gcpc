# gcpc

GCPC is a Google Cloud Print Connector.  That is, it is the piece of
software that interfaces your CUPS printers to the GCP, thus allowing
you to print from your Android devices anywhere you are.

Currently only the 1.0 version of the protocol is supported.

## Installation

Download the source code:

    $ git clone https://github.com/fourtytoo/gcpc.git

Download
[cups4j](http://www.cups4j.org/index.php?option=com_content&view=article&id=2&Itemid=7)
from its web page.  cups4j must be copied to the directory
./resources:

    $ mkdir resources
	$ fetch http://www.cups4j.org/...
    $ mv cups4j-<VERSION>.jar to resources

then it needs to be installed in the maven_repository:

    $ mkdir maven_repository
    $ mvn install:install-file -Dfile=resources/cups4j-<VERSION>.jar -DartifactId=cups4j -Dversion=<VERSION> -DgroupId=local -Dpackaging=jar -DlocalRepositoryPath=maven_repository -DcreateChecksum=true

Then you should be able to compile as usual:

    $ lein uberjar

## Usage

The jar file can be run:

    $ java -jar gcpc-0.1.0-standalone.jar

you should get a usage explanation.

With the first printer you configure you are asked a confirmation
through the visit of a web page.  A set of connection parameters are
thus obtained from the Google server and saved in the file `.gcpc` in
your home directory.

## Options

FIXME: listing of options this app accepts.

## Examples

...

## License

Copyright © 2016 Walter C. Pelissero <walter@pelissero.de>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
