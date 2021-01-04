# Experiment: Apache Camel

This project aims to experiment with [Apache Camel](https://camel.apache.org/), especially with its [Java DSL](https://camel.apache.org/manual/latest/java-dsl.html).

To do so, we will build a simple file flow that takes files into a source folder and extract, from those input files, CSV content that we dump in a target folder.

## Idea

![UML diagram](./uml/idea.png)

There is 1 entry :

- the source folder poller

There are 2 exits :

- the dump to target folder
- the error channel

## Disclaimer

I know that the *LogHandler* should not use `System.out.println` or `error.printStackTrace` ...
This is just a very raw handler to dump information in console =)

## Test me

```sh
mvn spring-boot:run
```

Once started, move a file from `data/source-example` to `data/source`.
You should see some log about what is going on in the system.
Also, you should notice some new files into `data/target`.

### PGP

To test PGP, let's follow [Camel documentation](https://camel.apache.org/components/latest/dataformats/pgp-dataformat.html):

```sh
Create your keyring, entering a secure password
gpg --gen-key

If you need to import someone elses public key so that you can encrypt a file for them.
gpg --import <filename.key

The following files should now exist and can be used to run the example
ls -l ~/.gnupg/pubring.gpg ~/.gnupg/secring.gpg
```

Still, If you're running a recent version of GPG, *pubring.gpg* and *secring.gpg* have been merged into *pubring.kbx*.

```
name : Foobar
email : foo.bar@acme.org
password : woot
```

You'll need to export keys:

```sh
# $PATH_TO_KEYS is PATH/TO/PROJECT/ROOT/data/pgp
gpg --export --armor foo.bar@acme.org > $PATH_TO_KEYS/public.gpg
gpg --export-secret-keys --armor foo.bar@acme.org > $PATH_TO_KEYS/private.gpg
# you'll need to enter password for exporting private key

ls data/pgp/pgp
private.gpg public.gpg
# those demo files are part of example files, feel free to regenerate them =)
```

Then you'll need to configure PGP in *application.properties*:
```
poc.pgp.private-key-path = file:./data/pgp/private.gpg
poc.pgp.password         = woot
```

Now we're set up, let's encrypt a file:

```sh
cd PATH/TO/PROJECT/ROOT

gpg --encrypt --output data/source/bla-encrypt.xlsx.gpg data/source-example/bla.xlsx
# set email previously defined as identity: foo.bar@acme.org

ls data/source
bla-encrypt.xlsx.gpg

ls data/target
# empty folder

mvn spring-boot:run
# enjoy log
#    -> got a message from input: bla-encrypt.xlsx.gpg
#    -> got a message from input: bla-encrypt.xlsx
#    -> got a message from input: bla-encrypt.csv
#    -> got a message from csv: bla-encrypt.csv
# quit application

ls data/target
bla-encrypt.csv
```
