(ns rocks.mygiftlist.model.user
  (:require [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
            [rocks.mygiftlist.type.user :as user]))

(defmutation upsert-user-on-auth0-id [user]
  (remote [_] true))
