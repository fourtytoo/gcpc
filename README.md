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

Before you do anything else you need to add at least a printer to the
cloud.

### Adding a printer

    $ java -jar gcpc-0.1.0-standalone.jar add <printer>

`<printer>` is the name of the printer on the local host.  If the
printer is on another CUPS host, you can use the form `printer@host`,
or even `printer@host:port`if your cups server is not listening
to port 631.

With the first printer you configure a set of access credentials are
obtained from Google's server and saved locally in the file `~/.gcpc`.
Although, this is sensitive information its loss doesn't compromise
your Google personal account, and it can be revoked at any time.

Also, with the first printer you configure, you are asked to visit a
confirmation page with your web browser.  There you may need to
identify yourself with your personal Google account and password.
Obviously these credentials are not saved on your hard disk; just the
robot refresh token is saved in the `.gcpc` file (see above).

## Options

FIXME: listing of options this app accepts.

## Examples

...

## Bugs

GCPC is still in its early development stages.  Bugs are to be
expected aplenty, and any error on your side will be met with an
unintelligible stacktrace.  Have fun hacking Clojure code!


## License

Copyright © 2016 Walter C. Pelissero <walter@pelissero.de>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
