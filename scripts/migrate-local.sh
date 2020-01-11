#!/bin/bash

pushd "$(git rev-parse --show-toplevel)"

docker run --rm --network mygiftlist_default -e POSTGRES_HOSTNAME=postgres -e POSTGRES_PORT=5432 -e POSTGRES_DB=postgres -v "$(pwd)/db_user.txt:/run/secrets/db_user" -v "$(pwd)/db_password.txt:/run/secrets/db_password" -v "$(pwd)/migrations:/flyway/sql" cpodonnell/mygiftlistrocks:migrate-latest

popd
