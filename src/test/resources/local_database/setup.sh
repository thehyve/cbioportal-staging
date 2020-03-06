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
    docker stop $DB_HOST >/dev/null || true && docker rm $DB_HOST >/dev/null || true
    docker network create cbio-net 2>/dev/null || true
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
        mysql:5.7 1>/dev/null

    while ! docker run --rm --net=cbio-net mysql:5.7 mysqladmin ping -u $DB_USER -p$DB_PASSWORD -h$DB_HOST --silent 2>/dev/null; do
        echo Waiting for cbioportal database to initialize...
        sleep 1
    done
}

run_ftp_server() {
    docker stop $FTP_HOST >/dev/null || true && docker rm $FTP_HOST >/dev/null || true
    docker network create ftp_network --driver bridge 2>/dev/null || true
    docker run -d \
        --name=$FTP_HOST \
        --net=ftp_network \
        -p 127.0.0.1:9922:22 \
        -v $TEST_HOME/e2e_studies:/home/testuser/studies:ro \
        -v $TEST_HOME/ftp_server/key.pub:/home/testuser/.ssh/keys/key.pub \
        -v $TEST_HOME/ftp_server/ssh_host_ed25519_key:/etc/ssh/ssh_host_ed25519_key \
        -v $TEST_HOME/ftp_server/ssh_host_rsa_key:/etc/ssh/ssh_host_rsa_key \
        atmoz/sftp \
        testuser:testuser:1001:100:/share,/studies 1>/dev/null
}

migrate_db() {
    echo Migrating database schema to most recent version ...
    docker run --rm \
        --net=cbio-net \
        -v "$TEST_HOME/local_database/portal.properties:/cbioportal/portal.properties:ro" \
        "$TEST_CBIOPORTAL_DOCKER_IMAGE" \
        python3 /cbioportal/core/src/main/scripts/migrate_db.py -y -p /cbioportal/portal.properties -s /cbioportal/db-scripts/src/main/resources/migration.sql
}

mkdir -p $WORKING_DIR
cd $WORKING_DIR

MYSQL_DATA_DIR="/tmp/mysql_data_integration_test"

build_and_run_database
migrate_db
run_ftp_server

# Make a dump of the database. It will be used to restore
# the database ot the initial state when tests have completed.
docker exec "$DB_HOST" mysqldump -u root -p$DB_USER cbioportal > $MYSQL_DUMP 2>/dev/null

# make test portal.properties available for Java integration test
cp "$TEST_HOME/local_database/portal.properties" "$WORKING_DIR"

cd - 1>/dev/null

exit 0
