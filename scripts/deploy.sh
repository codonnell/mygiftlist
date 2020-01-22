#!/bin/bash

set -e -o pipefail

pushd "$(git rev-parse --show-toplevel)"

./scripts/uberjar.sh

docker build -t cpodonnell/mygiftlistrocks:latest .

docker push cpodonnell/mygiftlistrocks:latest

scp ./docker-compose.prod.yaml mygiftlistrocks:~/docker-compose.mygiftlistrocks.yaml

ssh mygiftlistrocks "docker stack deploy -c docker-compose.mygiftlistrocks.yaml mygiftlistrocks"

popd
