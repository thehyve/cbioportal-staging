#
# Copyright (c) 2018 The Hyve B.V.
# This code is licensed under the GNU Affero General Public License (AGPL),
# version 3, or (at your option) any later version.
#

FROM openjdk:8
LABEL maintainers=" \
 Oleguer Plantalech Casals (The Hyve) <oleguer@thehyve.nl>, \
 Pim van Nierop (The Hyve) <pim@thehyve.nl> \
"

# install build and runtime dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
		maven \
		python3 \
		python3-pip && \
		pip3 install setuptools && \
		pip3 install awscli --upgrade --user

# install dependency for running docker client in this container
RUN apt-get update && apt-get install -y libltdl7 && rm -rf /var/lib/apt/lists/*

# get code
ENV STAGING_HOME=/cbioportal-staging
COPY . $STAGING_HOME
WORKDIR $STAGING_HOME

# prepare application.properties, so that this is take by default by spring framework
RUN cp $STAGING_HOME/src/main/resources/application.properties.EXAMPLE /application.properties

# run tests on code
RUN mvn test

# service to be started with default properties (can be overridden in docker run),
# taking also custom properties, if given at default -v location
ENTRYPOINT ["mvn", "spring-boot:run", "-Dspring.config.location=file:///application.properties"]
