#!/bin/bash

set -e -o pipefail

pushd "$(git rev-parse --show-toplevel)"

rm -rf resources/public/js/{manifest.edn,cljs-runtime,main.js}

npx shadow-cljs release :prod

clojure -A:depstar:uberjar

popd
