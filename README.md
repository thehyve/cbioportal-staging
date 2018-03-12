# cbioportal-staging
Staging app for automatically executing E(extract)T(transform)L(load) steps on study files stored in a central location (like a file share or S3) and meant to be loaded as studies in cBioPortal. This app executes the following workflow:

1. Extract files from central location
2. Transform files into cBioPortal study *staging files*
3. Validate the study *staging files* generated by step 2
4. Load the *staging files* into cBioPortal as new studies, or overwriting existing studies.

CI status: [![CircleCI](https://circleci.com/gh/thehyve/cbioportal-staging.svg?style=svg)](https://circleci.com/gh/thehyve/cbioportal-staging)
Build status: [![dockerhub](https://img.shields.io/docker/build/thehyve/cbioportal-staging.svg)](https://hub.docker.com/r/thehyve/cbioportal-staging)

## Setup

Configure your custom settings in `src/main/resources/application.properties`. See provided example file [src/main/resources/application.properties.EXAMPLE](src/main/resources/application.properties.EXAMPLE). Be sure to at least
configure `cloud.aws.region.static` there. 

## Build

To build and run tests execute:
```
mvn clean install
```
You can skip tests with extra argument `-DskipTests`.

### Docker build

Docker builds are made automatically at [https://hub.docker.com/r/thehyve/cbioportal-staging/](https://hub.docker.com/r/thehyve/cbioportal-staging/).
To manually build a local image use:

```
docker build -t cbio-staging .
```

## Run

To run use: 
```
./target/cbioportal-staging-*.jar
```

You can override application properties at runtime by adding them as parameters, for example:
```
./target/cbioportal-staging-*.jar --scan.cron="* * * * * *"
```
or link your own custom properties file, for example:
```
./target/cbioportal-staging-*.jar --spring.config.location=file:///custom/custom.properties
```

### Run in docker

To run in docker, use:

```
docker run -d --restart=always \
    --name=cbio-staging-container \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /usr/bin/docker:/usr/bin/docker \
    -v $PWD/custom.properties:/custom/custom.properties \
    cbio-staging
```

So, in the last `-v` parameter you can bind your own custom properties file.
This can be used to overlay the default parameters with your own parameters 
(spring will merge both files, so your custom file can contain only the subset of
properties that you want to overrid).
Furthermore, you can still override individual parameters on top of that by
adding them directly to the end of the docker command, e.g.:

```
docker run -d --restart=always \
    --name=cbio-staging-container \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /usr/bin/docker:/usr/bin/docker \
    -v $PWD/custom.properties:/custom/custom.properties \
    cbio-staging --scan.cron="* * * * * *"
```

## Yaml file format and location of study files
The app expects a yaml file in the working directory (path specified in `scan.location` of the `application.properties`) with the prefix `list_of_studies` in its name (for example, `list_of_studies_1.yaml`). This file contains all the file names, grouped by study, that will be required for the application to transform them into staging files suitable to be loaded into cBioPortal. The structure of the yaml file must be as follows:
```
study1:
    - /path/to/file1.txt
    - /path/to/file2.txt
study2:
    - /path/to/fileB.txt
    - /path/to/fileC.txt
```

The path to files should be relative to the `scan.location`. For example, if `file1.txt` is located in folder `files` inside `scan.location`, the file path in the yaml should be: `files/file1.txt`.

## Application properties
### Cron settings
We can configure the app to run as a cron job by using these parameters:
* `scan.cron`: expects a cron-like expression, extending the usual UN\*X definition to include triggers on the second as well as minute, hour, day of month, month and day of week.  e.g. `0 * * * * MON-FRI` means once per minute on weekdays (at the top of the minute - the 0th second). Other examples: `0 * * * *` (once every minute), `0 0 * * *` (once every hour), `0 0 0 * *` (once every day).
* `scan.cron.iterations`: number of times to repeat the cron (set to -1 to repeat indefinitely).

### Location settings
* `scan.location`: location to scan (local file system or S3 location). In this path is where the app expects to find the study files that it will download and pass on to the transformation step. Local file system example: `scan.location=file:///dir/staging_dir/`; S3 example: `scan.location=s3://bucket/staging_dir`.
* `etl.working.dir`: location of the working directory, that is the place where the app will save the study files retrieved from `scan.location` and also the generated staging files based on the study files.
* `central.share.location`: location where the app will save the different files that generates, such as validation reports or logs. This property can point to a local file system location or to a S3 bucket.
* `central.share.location.portal`: optional URL, in case the reports can also be found on a web portal. This will URL will be added to 
email notifications. For example: `https://s3.console.aws.amazon.com/s3/buckets/my_bucket/myreports_folder`.

### S3 vs local file system settings:
If any of the `*.location` attributes points to an S3 bucket, you will have to configure the following:

* `cloud.aws.region.static`: environment settings needed by S3 library. This is needed when `scan.location` points to S3 and running the tool outside EC2 environment.
* `cloud.aws.credentials.accessKey` and `cloud.aws.credentials.secretKey`: optional aws credentials for access to S3 bucket. Set these when aws credentials have not been configured on machine or if default aws credentials are different. Setting it here also improves performance of the S3 operations (probably because if these are not set, a slower trial and error route is chosen).

If **none** of the `*.location` attributes points to an S3 bucket, you will have to configure the following:
* `spring.autoconfigure.exclude`: set this to the list of AWS classes to skip in autoconfigure step when starting up the app. Set it to this: `spring.autoconfigure.exclude=org.springframework.cloud.aws.autoconfigure.context.ContextInstanceDataAutoConfiguration,org.springframework.cloud.aws.autoconfigure.context.ContextRegionProviderAutoConfiguration,org.springframework.cloud.aws.autoconfigure.context.ContextStackAutoConfiguration`


### cBioPortal settings
* `cbioportal.mode`: must be `local` or `docker`,  depending whether the app will run with a local cBioPortal or a dockerized cBioPortal.
* `cbioportal.docker.image`, `cbioportal.docker.network`: Docker image and network names for the dockerized cBioPortal. These parameters are only required if the `cbioportal.mode` is `docker`.
* `cbioportal.docker.cbio.container`: name of the running cBioPortal container (e.g. to be restarted after studies are loaded).

### Extractor settings
* `time.attempt`: minutes before trying to find a file specified by the yaml file that has not been found. This will be tried 5 times.

### Transformer settings
* `transformation.command.script`: full transformation command, except input and output (-i and -o parameters).
* `transformation.command.location`: local path where the transformation command will be executed.

### Validation settings
* `validation.level`: sets the threshold for loading studies after validation. It has two options: `WARNING` (to already abort loading step if one or more WARNINGs is found during validation step), and `ERROR` (to only abort loading if one or more ERRORs is found during validation step).

### Mail properties
The app sends emails to keep the user informed about the status of the tasks performed by the app. In order to do that, we need the following parameters to be set:
* `mail.admin.user`: address where the app will send the emails to.
* `mail.app.user` and `mail.app.password`: credentials of the app email. Those parameters should not be changed.
* `mail.smtp.host`: email host, in Gmail is `smtp.gmail.com`.
* `mail.debug`: boolean, if set to `true` prints debugging logs in the screen. 
* `mail.transport.protocol`: email transport protocol, usually `smtp`.
* `mail.smtp.port`: smtp port, in Gmail is `465`.
* `mail.smtp.auth`: boolean, if set to `true` requires to log in for the mail app email before sending messages. In general is set to `true`.
* `mail.smtp.ssl.enable`: enable SSL if set to `true`. In general is set to `true`.
* `mail.smtp.starttls.enable` enable TLS if set to `true`. In general is set to `true`.
