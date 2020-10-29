# cBioPortal Staging Application Usage Guide

The cBioPortal Staging Application automatically executes E(extract)T(transform)L(load) steps on study files stored in a central location (like a file share or S3) and loads them as studies in cBioPortal. The app executes the following workflow:

1. Extracts files from central location
2. Transforms files into cBioPortal study *staging files*
3. Validates the study *staging files* generated by step 2
4. Loads the *staging files* into cBioPortal as new studies, or overwriting existing studies.

CI status: [![CircleCI](https://circleci.com/gh/thehyve/cbioportal-staging.svg?style=svg)](https://circleci.com/gh/thehyve/cbioportal-staging)
Build status: [![dockerhub](https://img.shields.io/docker/build/thehyve/cbioportal-staging.svg)](https://hub.docker.com/r/thehyve/cbioportal-staging)

## Table of contents


<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [cBioPortal Staging Application Usage Guide](#cbioportal-staging-application-usage-guide)
  - [Table of contents](#table-of-contents)
  - [Setup](#setup)
  - [Docker usage (recommended)](#docker-usage-recommended)
    - [Build](#build)
    - [Run](#run)
  - [Local usage](#local-usage)
    - [Build](#build-1)
    - [Run](#run-1)
  - [Strategy for discovering study files](#strategy-for-discovering-study-files)
      - [Yaml file format and location of study files](#yaml-file-format-and-location-of-study-files)
      - [Study-specific directories for location of study files](#study-specific-directories-for-location-of-study-files)
    - [Running with AWS remote file system](#running-with-aws-remote-file-system)
  - [Application properties](#application-properties)
    - [Extractor settings](#extractor-settings)
    - [Transformer settings](#transformer-settings)
    - [Validation and Loader settings](#validation-and-loader-settings)
    - [Properties for 'docker' application profile](#properties-for-docker-application-profile)
    - [Properties for 'local' application profile](#properties-for-local-application-profile)
    - [Reporting location settings](#reporting-location-settings)
    - [S3 file system settings](#s3-file-system-settings)
    - [SFTP file system settings](#sftp-file-system-settings)
    - [Mail properties](#mail-properties)
    - [Log setttings](#log-setttings)
    - [Debug settings](#debug-settings)
    - [Update cbioportal version used for integration tests](#update-cbioportal-version-used-for-integration-tests)
    - [Other](#other)
  - [Debug of integration tests](#debug-of-integration-tests)

<!-- /code_chunk_output -->


## Setup

Configure your custom settings in `src/main/resources/application.properties` or run
the app wit extra parameter like for example `--spring.config.location=file:///custom/custom.properties`
(see **Run** section below). For details on how to configure, see provided example
file [src/main/resources/application.properties](src/main/resources/application.properties.EXAMPLE)
and **Application properties** section below.

## Docker usage (recommended)

### Build

Docker builds are made automatically at [https://hub.docker.com/r/thehyve/cbioportal-staging/](https://hub.docker.com/r/thehyve/cbioportal-staging/).
To manually build a local image use:

```sh
docker build -t cbio-staging .
```

### Run

To run in Docker, use:

```sh
docker run -d --rm \
    --name=cbio-staging \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /usr/bin/docker:/usr/bin/docker \
    -v $PWD/custom.properties:/custom/custom.properties \
    cbio-staging
```

So, in the last `-v` parameter you can bind your own custom properties file.
This can be used to overlay the default parameters with your own parameters
(spring will merge both files, so your custom file can contain only the subset of
properties that you want to override).
Furthermore, you can still override individual parameters on top of that by
adding them directly to the end of the docker command, e.g.:

```sh
docker run -d --rm \
    --name=cbio-staging \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /usr/bin/docker:/usr/bin/docker \
    -v $PWD/custom.properties:/custom/custom.properties \
    cbio-staging --scan.cron="0 * * * * *"
```

## Local usage

### Build

To build and run unit tests execute:

```sh
mvn clean install
```

You can skip unit tests with extra argument `-DskipUnitTests`:

```sh
mvn clean install -DskipUnitTests
```

To build and run unit tests __and__ integration tests execute:

```sh
mvn clean install -P integration-test
```

To build and run only integration tests execute:

```sh
mvn clean install -P integration-test -DskipUnitTests
```

:warning: When running integration tests the cBioPortal database
should be initialized and running. For this execute:

```sh
./src/test/resources/local_database/setup.sh
```

### Run

To run use:

```sh
./target/cbioportal-staging-*.jar
```

You can override application properties at runtime by adding them as parameters,
for example:

```sh
./target/cbioportal-staging-*.jar --scan.cron="0 * * * * *"
```

or link your own custom properties file, for example:

```sh
./target/cbioportal-staging-*.jar --spring.config.location=file:///custom/custom.properties
```

## Strategy for discovering study files

The application supports different modes of discovery of study resources
on at the scanned location. The default method is via a yaml-file that lists
resources per study. An alternative are study-specific directories; this strategy
can be activated by passing the `scan.studydir` profile to the application.

#### Yaml file format and location of study files

The app expects a yaml file in the working directory (path specified in `scan.location`
of the `application.properties`) with the prefix `list_of_studies` in its name
(for example, `list_of_studies_1.yaml`). This file contains all the file names,
grouped by study, that will be required for the application to transform them
into staging files suitableto be loaded into cBioPortal. The structure of the
yaml file must be as follows:

```yaml
study1:
    - path/to/file1.txt
    - path/to/file2.txt
study2:
    - path/to/fileB.txt
    - path/to/fileC.txt
```

Files in the same "study" should be in the same relative paths. For example, this
is *not* allowed:

```yaml
study1:
    - path/to/file1.txt
    - path/to/file2.txt
    - path_2/to/file3.txt
```

To correct it, change `/path_2/to/file3.txt` to `/path/to/file3.txt` in this case.

The path to files should be *relative to* the `scan.location`. For example, if
`file1.txt` is located in folder `files` inside `scan.location`, the file path
in the yaml should be: `files/file1.txt`.

#### Study-specific directories for location of study files

When using the `scan.studydir` profile, the application will look in the working
directory (path specified in scan.location of the application.properties) for
folders that each contain files beloning to a single study. The name of the
folder is used as the study identifier.

```sh
working directory
└───study1
    | file1.txt
    | file2.txt
    | ...
└───study2
    | file1.txt
    | file2.txt
    | ...
```

In the example above the study identifiers will be _study1_ and _study2_.

### Running with AWS remote file system

To start the staging application with the `aws` maven profile:

<pre>
mvn spring-boot:run <b>-P aws</b>
</pre>

or, when running in docker:

<pre>
docker run -d --restart=always \
    --name=cbio-staging-container \
    ...
    cbio-staging <b>-P aws</b>
</pre>

Make sure to add the AWS credentials to the custom properties file (see
[S3 file system settings](#s3-file-system-settings)).

## Application properties

### Extractor settings

We can configure the app to run as a cron job by using these parameters:

- `scan.cron`: expects a cron-like expression, extending the usual UN\*X definition to include triggers on the second as well as minute, hour, day of month, month and day of week.  e.g. `0 * * * * MON-FRI` means once per minute on weekdays (at the top of the minute - the 0th second). Other examples: `0 * * * *` (once every minute), `0 0 * * *` (once every hour), `0 0 0 * *` (once every day).
- `scan.cron.iterations`: number of times to repeat the cron (set to -1 to repeat indefinitely). Default is 1 (once).
- `scan.location`: location to scan (local file system or S3 location). In this path is where the app expects to find the study files that it will download and pass on to the transformation step. Local file system example: `scan.location=file:///dir/staging_dir/`; S3 example: `scan.location=s3://bucket/staging_dir`.
- `scan.retry.time`: minutes before trying to find a file specified by the yaml file that has not been found. This will be tried 5 times.
- `scan.extract.folders`: if used, it will only run the staging app for the specific folders (studies) placed inside the `scan.location` place. For example, to only load `study2` and `study3`, contained in `study2_dir` and `study3_dir` folders, set the property like this: `scan.extract.folders=study2_dir,study3_dir`. If the property is commented out, the app will load all folders contained in `scan.location`.
- `etl.working.dir`: location of the working directory, that is the place where the app will save the study files retrieved from `scan.location` and also the generated staging files based on the study files.
- `etl.dir.format`: format of the path within the working directory for the extracted files. There are three options, each value defining the path where the study will be saved: `timestamp/study_id`; `study_id/timestamp`; and `study_id/version`, where `version` is the value saved in the "version" property of the study object. 
- `scan.ignore.file`: when specified, all files listed in this file will be excluded
from the scan. Each file should be represented on a single line. Only exact matched will
be excluded. Wildcards are not supported.
- `scan.ignore.appendonsuccess`: when set to true, all study files of succesfully loaded
studies are appended to the `scan.ignore.file`. This prevents ETL to be triggered when
files are not removed from the `scan.location`.

### Transformer settings

- `transformation.command.script`: full transformation command, except input and output (-i and -o parameters). First element must be the absolute path to the transformation script; the path must contain a resource type prefix (e.g. `file:`).
- `transformation.command.script.docker.image`: if the transformation command runs in Docker, add here the Docker image name. The app will call the script in the path provided in `transformtion.command.script`. If your command is executed at the entrypoint, you can comment `transformation.command.script` out.
- `transformation.skip`: set this parameter to `true` if you want to skip the transformation step.
- `transformation.directory`: resource path to directory where transformed study files are placed after transformation. When not set, transformed files are placed in the 'staging' subdirectory of study folders in the `etl.working.dir`.
- `transformation.metafile.check`: set this parameter to to `false` if you want to transform also studies containing a meta_study.txt file (skipped by default even if `transformation.skip` is `false`).

### Validation and Loader settings

- `validation.level`: sets the threshold for loading studies after validation. It has two options: `WARNING` (to already abort loading step if one or more WARNINGs is found during validation step), and `ERROR` (to only abort loading if one or more ERRORs is found during validation step).

### Properties for 'docker' application profile

- `cbioportal.docker.image`, `cbioportal.docker.network`: Docker image and network names for the dockerized cBioPortal.
- `cbioportal.docker.cbio.container`: name of the running cBioPortal container (e.g. to be restarted after studies are loaded).
- `cbioportal.docker.network`: docker network where cBioPortal is placed.
- `cbioportal.docker.properties`: path to the `portal.properties` of the cBioPortal container.

### Properties for 'compose' application profile

- `cbioportal.compose.service`: name for cbioportal service in compose file
- `cbioportal.compose.cbioportal.extensions`: list of docker compose extension files for the cbioportal container (command-separated list).

### Properties for 'local' application profile

- `portal.source`: path to portal source

### Reporting location settings

- `central.share.location`: location where the app will save the different files that generates, such as validation reports or logs. This property can point to a local file system location or to a S3 bucket.
- `central.share.location.web.address`: optional URL, in case the reports can also be found on a web portal. This will URL will be added to
email notifications. For example: `https://s3.console.aws.amazon.com/s3/buckets/my_bucket/myreports_folder`.

### S3 file system settings

Staging app can scan on an S3 bucket. To activate this feature set property `scan.location.type` to _aws_ (and start
the app with the maven `aws` active profile).
You will must configure the following:

- `cloud.aws.region.static`: environment settings needed by S3 library. This is needed when `scan.location` points to S3 and running the tool outside EC2 environment.
- `cloud.aws.credentials.accessKey` and `cloud.aws.credentials.secretKey`: optional aws credentials for access to S3 bucket. Set these when aws credentials have not been configured on machine or if default aws credentials are different. Setting it here also improves performance of the S3 operations (probably because if these are not set, a slower trial and error route is chosen).

### SFTP file system settings

Staging app can scan on a SFTP server. To activate this feature set property `scan.location.type` to _sftp_. Also, configure the
following properties:

- `ftp.host`: host name of SFTP server. Example: localhost
- `ftp.port`: port of SFTP SERVER. Example: 22
- `ftp.user`: username for account on SFTP SERVER.
- `ftp.password`: user password for account on SFTP SERVER.
- `sftp.privateKey`: file that contains RSA private key information for account. Example: file:///keys/my_privvate_key
- `sftp.privateKeyPassphrase`: passphrase for RSA private key.

### Mail properties

The app sends emails to keep the user informed about the status of the tasks performed by the app. In order to do that, we need the following parameters to be set:

- `mail.enable`: when _true_ will activate progress reports sent via email.
- `mail.to`: comma-separated addresses where the app will send the emails to.
- `mail.from`: email address to be used as sender/"from"
- `mail.smtp.user`: smtp user name.
- `mail.smtp.password`: (optional) respective credentials of the app email
(corresponding to `mail.smtp.user`). This **can be empty** if a local email
service (e.g. local postfix) is used.
- `mail.smtp.host`: email host, in Gmail is `smtp.gmail.com`.
- `mail.debug`: boolean, if set to `true` prints debugging logs in the screen.
- `mail.transport.protocol`: email transport protocol, usually `smtp`.
- `mail.smtp.port`: smtp port, in Gmail is `465`, for local (e.g. postfix) it is
usually `25`
- `mail.smtp.auth`: boolean, if set to `true` requires to log in for the mail
app email before sending messages. In general is set to `true`.
- `mail.smtp.ssl.enable`: enable SSL if set to `true`. In general is set to `true`.
- `mail.smtp.starttls.enable` enable TLS if set to `true`. In general is set to `true`.
- `study.curator.emails`: comma-separated emails of the users which will get
automatic access to studies loaded, and also will receive a copy of all emails
sent by the staging app.

### Log setttings

- `log.enable`: when _true_ will activate progress reports added to a log file.
- `log.file`: file that is used to capture log messages.
- `log.format`: format of the log file; can be 'text' or 'html'.
- `log.html.prepend`: when _true_ will add new log messages at the top of the
log file instead of appending it to the bottom. This option is only available
for a 'html' formatted log file.

### Debug settings

- `logging.level.root`: default log level / you can use this to debug and
troubleshoot issues (default `INFO`)
- `debug.mode`: if debug mode is set to `true`, emails will only be sent to the
`study.curator.emails` (and not to the `mail.smtp.user`)

### Update cbioportal version used for integration tests

Update to the same Dockerhub image reference (e.g., _cbioportal/cbioportal-3.1.4_):

- `test.cbioportal.docker.image` property in _pom.xml_
- `cbioportal.docker.image` in _src/test/resources/integration_test.properties_
- `TEST_CBIOPORTAL_DOCKER_IMAGE` in _.circleci/config.yml_

### Other

- `study.authorize.command_prefix`:  add a command for the study authorizer layer,
otherwise leave it empty (or comment it out). The staging app will append the
`study id` and the `study.curator.emails` to this command.
- `server.alias`: recognizable name for the server, appears in the emails, e.g. `DEV`.

## Debug of integration tests

To debug integration tests, you need first to change the `TEST_HOME` varible in `dev_setup.sh`. Then, execute the script to build the test database:
```
./src/test/resources/local_database/dev_setup.sh
```

After building the database, you can set breakpoints and run/debug the integration tests.
