(ns rocks.mygiftlist.server-components.middleware
  (:require [mount.core :refer [defstate]]
            [rocks.mygiftlist.server-components.db :as db]
            [rocks.mygiftlist.server-components.pathom :as parser]
            [rocks.mygiftlist.config :as config]
            [com.fulcrologic.fulcro.server.api-middleware :refer [handle-api-request
                                                                  wrap-transit-params
                                                                  wrap-transit-response]]
            [ring.middleware.jwt :as jwt]))

(defn not-found-handler [req]
  (assoc-in (resp/resource-response "public/index.html")
    [:headers "Content-Type"] "text/html"))

(defn wrap-api [handler uri pool]
  (fn [request]
    (if (= uri (:uri request))
      (handle-api-request
        (:transit-params request)
        (fn [tx] (parser/parser {:ring/request request
                                 ::db/pool pool} tx)))
      (handler request))))

(defstate handler
  :start
  (-> not-found-handler
    (wrap-api "/api" db/pool)
    (jwt/wrap-jwt {:alg          :RS256
                   :jwk-endpoint (:jwk-endpoint (config/get-config config/environment))})
    wrap-transit-params
    wrap-transit-response
    (wrap-defaults (assoc api-defaults :static {:resources "public"}))))
