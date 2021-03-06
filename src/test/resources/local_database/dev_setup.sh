#!/usr/bin/env bash

set -e
set -u # unset variables throw error
set -o pipefail # pipes fail when partial command fails

export FTP_HOST=sftp
export DB_HOST=cbioDB
export DB_USER=cbio
export DB_PASSWORD=P@ssword1
export DB_PORTAL_DB_NAME=cbioportal
export WORKING_DIR=/tmp/staging-integration-test/
export TEST_HOME=/home/pnp300/git/cbioportal-staging/src/test/resources/
export TEST_CBIOPORTAL_DOCKER_IMAGE=cbioportal/cbioportal:3.1.4
export MYSQL_DUMP=/tmp/mysql.dump
./src/test/resources/local_database/setup.sh