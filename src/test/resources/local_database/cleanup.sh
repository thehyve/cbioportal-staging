#!/usr/bin/env bash

set -e
set -u # unset variables throw error
set -o pipefail # pipes fail when partial command fails

# Retore the initial state of the database by
# importing the dump created in setup.sh.
docker exec "$DB_HOST" mysql -u root -p$DB_USER cbioportal < $MYSQL_DUMP

docker stop $DB_HOST 2> /dev/null
rm -rf /tmp/staging-integration-test 2> /dev/null || true

exit 0
