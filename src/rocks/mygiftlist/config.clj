(ns rocks.mygiftlist.config
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [aero.core :as aero]))

(defmethod aero/reader 'docker-secret
  [_ _ value]
  (try (str/trim (slurp (io/file value)))
       (catch Throwable _
         nil)))

(def ^:private config (aero/read-config (io/resource "config.edn")))

(def database-spec (:database-spec config))

(def jwk-endpoint (:jwk-endpoint config))

(def port (:port config))

(comment
  config
  )
