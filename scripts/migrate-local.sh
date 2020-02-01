#!/bin/bash

pushd "$(git rev-parse --show-toplevel)"

docker build -f Dockerfile.flyway -t cpodonnell/mygiftlistrocks:migrate-local .

docker run --rm \
       --network mygiftlist_default \
       -e POSTGRES_USER_FILE=/db/secrets/db_user \
       -e POSTGRES_PASSWORD_FILE=/db/secrets/db_password \
       -e POSTGRES_HOSTNAME=postgres \
       -e POSTGRES_PORT=5432 \
       -e POSTGRES_DB=postgres \
       -v "$(pwd)/db_user.txt:/db/secrets/db_user" \
       -v "$(pwd)/db_password.txt:/db/secrets/db_password" \
       cpodonnell/mygiftlistrocks:migrate-local

popd
