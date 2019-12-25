(ns rocks.mygiftlist.config
  (:require [datomic.ion :as ion]))

(def environment
  "The environment this configuration is for. Gets overwritten for local development."
  :prod)

(defmulti get-config (fn [env] env))

(def get-ion-params
  (memoize #(ion/get-params {:path "/datomic-shared/prod/mygiftlistrocks/"})))

(defmethod get-config :prod [_]
  (get-ion-params))

(comment
  )
