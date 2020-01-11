(ns rocks.mygiftlist.model.user
  (:require [com.fulcrologic.fulcro.mutations :refer [defmutation]]))

(defmutation upsert-user-on-auth0-id [user]
  (remote [_] true))
