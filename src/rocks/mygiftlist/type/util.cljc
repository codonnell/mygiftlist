(ns rocks.mygiftlist.type.util
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::nonblank-string (s/and string? (complement str/blank?)))
