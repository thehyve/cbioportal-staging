# cbioportal-staging
Staging app for automatically loading studies into cBioPortal.

## Setup

Configure your custom setting in `src/main/resources/application.properties`. Be sure to at least
configure `cloud.aws.region.static` there. 

## Build

To build run:
```
mvn clean install
```
## Run

To run use: 
```
./target/cbioportal-staging-0.0.1-SNAPSHOT.jar
```

You can overwrite application properties at runtime by adding them as parameters, for example:
```
./target/cbioportal-staging-*.jar --scan.cron="* * * * * *"
```

## Yaml file format and location of study files
The app expects a yaml file in the working directory (path specified in `etl.working.dir` of the `application.properties`) with the prefix `list_of_studies` in its name (for example, `list_of_studies_1.yaml`). This file contains all the file names, grouped by study, that will be required for the application to transform them into staging files suitable to be loaded into cBioPortal. The structure of the yaml file must be as follows:
```
study1:
    - file1.txt
    - file2.txt
study2:
    - fileB.txt
    - fileC.txt
```
In the example above, `study1` and `study2` are the names of the studies, and `file1.txt`, `file2.txt`, `fileB.txt` and `fileC.txt` are the names of the study files. The app expects to find `file1.txt` and `file2.txt` inside a folder called `study1`, and `fileB.txt` and `fileC.txt` inside a folder called `study2`. Those study folders are expected to be found in the `scan.location` specified in `application.properties`.

## Application properties
### Cron settings
We can configure the app to run as a cron job by using these parameters:
* `scan.cron`: expects a cron-like expression, extending the usual UN\*X definition to include triggers on the second as well as minute, hour, day of month, month and day of week.  e.g. `0 * * * * MON-FRI` means once per minute on weekdays (at the top of the minute - the 0th second). Other examples: `0 * * * *` (once every minute), `0 0 * * *` (once every hour), `0 0 0 * *` (once every day).
* `scan.cron.iterations`: number of times to repeat the cron (se to -1 to repeat indefinitely).

### Location settings
* `scan.location`: location to scan (local file system or S3 location). In this path is where the app expects to find the study files that will transform.
* `cloud.aws.region.static`: environment settings needed by S3 library. This is needed when `scan.location` points to S3 and running the tool outside EC2 environment.
* `cloud.aws.credentials.accessKey` and `cloud.aws.credentials.secretKey`: optional aws credentials for access to S3 bucket. Set these when aws credentials have not been configured on machine or if default aws credentials are different. Setting it here also improves performance of the S3 operations (probably because if these are not set, a slower trial and error route is chosen).
* `etl.working.dir`: location of the working directory, that is the place where the app will save the study files retrieved from `scan.location`.
* `central.share.location`: location where the app will save the different files that generates, such as validation reports or logs.

### cBioPortal settings
* `cbioportal.mode`: must be `local` or `docker`,  depending whether the app will run with a local cBioPortal or a dockerized cBioPortal.
* `cbioportal.docker.image`, `cbioportal.docker.network` and `cbioportal.container.name`: Docker image, network and container names for the dockerized cBioPortal. These parameters are only required if the `cbioportal.mode` is `docker`.
* `tomcat.command`: the command-line name for Tomcat (for example, `catalina`). This is only required if the `cbioportal.mode` is `local`.

### Validation settings
* `validation.level`: sets the threshold for loading studies after validation. It has two options: `Warnings` (to load studies with no warnings or errors), and `Errors` (to load studies with no errors, but that can contain warnings).

### Mail properties
The app sends emails to keep the user informed about the status of the tasks performed by the app. In order to do that, we need the following parameters to be set:
* `mail.admin.user`: address where the app will send the emails.
* `mail.app.user` and `mail.app.password`: credentials of the app email. Those parameters should not be changed.
* `mail.smtp.host`: email host, in Gmail is `smtp.gmail.com`.
* `mail.debug`: boolean, if set to `true` prints debugging logs in the screen. 
* `mail.transport.protocol`: email transport protocol, usually `smtp`.
* `mail.smtp.port`: smtp port, in Gmail is `465`.
* `mail.smtp.auth`: boolean, if set to `true` requires to log in for the mail app email before sending messages. In general is set to `true`.
* `mail.smtp.ssl.enable`: enable SSL if set to `true`. In general is set to `true`.
* `mail.smtp.starttls.enable` enable TLS if set to `true`. In general is set to `true`.
