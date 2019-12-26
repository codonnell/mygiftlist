(ns rocks.mygiftlist.ion.http
  (:require
   [clojure.java.io :as io]
   [rocks.mygiftlist.ion :as ion]
   [rocks.mygiftlist.ion.edn :as edn]
   [rocks.mygiftlist.server-components.middleware :as middleware]
   [datomic.ion.lambda.api-gateway :as apigw]))

(defn api-handler
  "Web handler that returns info about items matching type."
  [{:keys [headers body] :as request}]
  ((middleware/handler) request))

(def api-lambda-proxy
  (apigw/ionize api-handler))
