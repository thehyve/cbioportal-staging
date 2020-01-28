#!/usr/bin/env bash

set -e
set -u # unset variables throw error
set -o pipefail # pipes fail when partial command fails

download_db_seed() {
    # When the database has been initialized (the 'mysql' dir is present),
    # downloading of seed and schema is not needed.
    if ! [[ -e "$MYSQL_DATA_DIR/mysql" ]]; then
        if ! [[ -e cgds.sql ]]; then
            curl https://raw.githubusercontent.com/cBioPortal/cbioportal/v2.0.0/db-scripts/src/main/resources/cgds.sql > cgds.sql
        fi
        if ! [[ -e seed.sql.gz ]]; then
            curl https://raw.githubusercontent.com/cBioPortal/datahub/master/seedDB/seed-cbioportal_hg19_v2.7.3.sql.gz > seed.sql.gz
        fi
    fi
}

build_and_run_database() {
    # create local database from with cbioportal db and seed data
    download_db_seed
    docker stop $DB_HOST && docker rm $DB_HOST
    docker network create cbio-net >/dev/null || true
    docker run -d \
        --name=$DB_HOST \
        --net=cbio-net \
        -e MYSQL_ROOT_PASSWORD=$DB_USER \
        -e MYSQL_USER=$DB_USER \
        -e MYSQL_PASSWORD=$DB_PASSWORD \
        -e MYSQL_DATABASE=$DB_PORTAL_DB_NAME \
        -p 127.0.0.1:3306:3306 \
        -v "$MYSQL_DATA_DIR:/var/lib/mysql/" \
        -v "/tmp/staging-integration-test/cgds.sql:/docker-entrypoint-initdb.d/cgds.sql:ro" \
        -v "/tmp/staging-integration-test/seed.sql.gz:/docker-entrypoint-initdb.d/seed_part1.sql.gz:ro" \
        mysql:5.7

    while ! docker run --rm --net=cbio-net mysql:5.7 mysqladmin ping -u $DB_USER -p$DB_PASSWORD -h$DB_HOST --silent >/dev/null; do
        echo Waiting for cbioportal database to initialize...
        sleep 1
    done
}

migrate_db() {
    # TODO: remove hard coded reference to cbioportal version --> cbioportal/cbioportal:3.1.4
    echo Migrating database schema to most recent version ...
    docker run --rm \
        --net=cbio-net \
        -v "$TEST_HOME/portal.properties:/cbioportal/portal.properties:ro" \
        "$TEST_CBIOPORTAL_DOCKER_IMAGE" \
        python3 /cbioportal/core/src/main/scripts/migrate_db.py -y -p /cbioportal/portal.properties -s /cbioportal/db-scripts/src/main/resources/migration.sql
}

mkdir -p $WORKING_DIR
cd $WORKING_DIR

MYSQL_DATA_DIR="/tmp/mysql_data_integration_test"

build_and_run_database
migrate_db

# make test portal.properties available for Java integration test
cp "$TEST_HOME/portal.properties" "$WORKING_DIR"

cd -

exit 0
