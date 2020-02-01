#!/bin/bash

set -e -o pipefail

pushd "$(git rev-parse --show-toplevel)"

docker build -f Dockerfile.flyway -t cpodonnell/mygiftlistrocks:migrate-latest .

docker push cpodonnell/mygiftlistrocks:migrate-latest

kubectl apply -f kubernetes/migrate-db-job.yaml

popd
