#!/bin/bash

set -x -e -o pipefail

pushd "$(git rev-parse --show-toplevel)"

npx shadow-cljs release prod

aws s3 cp resources/public/js/main.js s3://mygiftlistrocks/js/main.js --acl public-read

aws s3 cp resources/public/css/style.css s3://mygiftlistrocks/css/style.css --acl public-read

popd
