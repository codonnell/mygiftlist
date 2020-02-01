#!/bin/bash

set -e -o pipefail

pushd "$(git rev-parse --show-toplevel)"

./scripts/uberjar.sh

docker build -t cpodonnell/mygiftlistrocks:latest .

docker push cpodonnell/mygiftlistrocks:latest

# TODO: We need to start using commit sha tags to really update
kubectl apply -f kubernetes/backend.yaml

popd
