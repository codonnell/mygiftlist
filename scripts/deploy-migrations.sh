#!/bin/bash

set -e -o pipefail

pushd "$(git rev-parse --show-toplevel)"

docker build -f Dockerfile.flyway -t cpodonnell/mygiftlistrocks:migrate-latest .

docker push cpodonnell/mygiftlistrocks:migrate-latest

ssh mygiftlistrocks "docker service create -e POSTGRES_HOSTNAME=postgres -e POSTGRES_PORT=5432 -e POSTGRES_DB=mygiftlistrocks --network mygiftlistrocks_backend-private --secret db_user --secret db_password --restart-condition none --name db-migrator cpodonnell/mygiftlistrocks:migrate-latest </dev/null >/dev/null 2>&1 &"

until [ "$(ssh mygiftlistrocks "docker service ps db-migrator" | grep Complete)" ]; do
    sleep 5
done

ssh mygiftlistrocks "docker service rm db-migrator"

popd
