#
# Copyright (c) 2018 The Hyve B.V.
# This code is licensed under the GNU Affero General Public License (AGPL),
# version 3, or (at your option) any later version.
#

FROM tomcat:8-jre8

# install build and runtime dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
		git \
		maven \
		openjdk-8-jdk

# clone pipeline
ENV PORTAL_HOME=/cbioportal-staging
RUN git clone --depth 1 'https://github.com/thehyve/cbioportal-staging.git' $PORTAL_HOME
WORKDIR $PORTAL_HOME
RUN git fetch --depth 1 https://github.com/thehyve/cbioportal-staging.git la_canya \
       && git checkout a846d3a9ed9d94ad4f294fa803d1a13c1f0d3eb3

RUN mvn clean install