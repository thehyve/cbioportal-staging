# cbioportal-staging
Staging app for automatically executing E(extract)T(transform)L(load) steps on study files stored in a central (staging) location (like a file share or S3) and meant to be loaded as studies in cBioPortal.

## Setup

Configure your custom settings in `src/main/resources/application.properties`. See provided example file [src/main/resources/application.properties.EXAMPLE](src/main/resources/application.properties.EXAMPLE). Be sure to at least
configure `cloud.aws.region.static` there. 

## Build

To build and run tests execute:
```
mvn clean install
```
You can skip tests with extra argument `-DskipTests`.

## Run

To run use: 
```
./target/cbioportal-staging-*.jar
```

You can overwrite application properties at runtime by adding them as parameters, for example:
```
./target/cbioportal-staging-*.jar --scan.cron="* * * * * *"
```
