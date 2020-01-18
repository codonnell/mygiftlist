(ns rocks.mygiftlist.type.gift-list
  (:require [clojure.spec.alpha :as s]
            [rocks.mygiftlist.type.util :as type.util]))

(s/def ::name ::type.util/nonblank-string)
