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
