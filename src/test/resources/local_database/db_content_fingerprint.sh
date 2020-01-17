#!/usr/bin/env bash

set -e 
set -u
set -o pipefail

dir=$PWD
md5_migration_sql=$(docker run --rm "$TEST_CBIOPORTAL_DOCKER_IMAGE" sh -c 'md5sum /cbioportal/db-scripts/src/main/resources/migration.sql | sed "s/\s.*$//"')
cd $dir

echo "$md5_migration_sql"
