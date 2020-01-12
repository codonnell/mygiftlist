(ns rocks.mygiftlist.type.gift
  (:require [clojure.spec.alpha :as s]
            [rocks.mygiftlist.type.util :as type.util]))

(s/def ::name ::type.util/nonblank-string)
