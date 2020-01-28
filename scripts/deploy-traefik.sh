#!/bin/bash

set -e -o pipefail

pushd "$(git rev-parse --show-toplevel)"

docker build -f Dockerfile.traefik -t cpodonnell/mygiftlistrocks-traefik:latest .

docker push cpodonnell/mygiftlistrocks-traefik:latest

scp ./docker-compose.traefik.yaml mygiftlistrocks:~/docker-compose.traefik.yaml

ssh mygiftlistrocks "docker stack deploy -c docker-compose.traefik.yaml traefik"

popd
