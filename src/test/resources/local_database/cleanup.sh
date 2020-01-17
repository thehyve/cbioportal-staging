#!/usr/bin/env bash

set -e 
set -u # unset variables throw error
set -o pipefail # pipes fail when partial command fails

docker stop $DB_HOST 2> /dev/null
rm -rf /tmp/staging-integration-test 2> /dev/null || true

exit 0
