(ns rocks.mygiftlist.server-components.middleware
  (:require
   [rocks.mygiftlist.server-components.pathom :refer [parser]]
   [rocks.mygiftlist.config :as config]
   [com.fulcrologic.fulcro.server.api-middleware :refer [handle-api-request
                                                         wrap-transit-params
                                                         wrap-transit-response]]
   [ring.middleware.jwt :as jwt]
   [taoensso.timbre :as log]))

(def ^:private not-found-handler
  (fn [req]
    {:status  404
     :headers {"Content-Type" "text/plain"}
     :body    "Not found"}))

(defn wrap-api [handler uri]
  (fn [request]
    (if (= uri (:uri request))
      (handle-api-request
        (:transit-params request)
        (fn [tx] (parser {:ring/request request} tx)))
      (handler request))))

(defn spy [handler]
  (fn [req]
    (log/debug req)
    (handler req)))

(defn api-middleware [handler]
  (-> handler
    (wrap-api "/api")
    (jwt/wrap-jwt {:alg          :RS256
                   :jwk-endpoint (:jwk-endpoint (config/get-config config/environment))})
    spy
    wrap-transit-params
    wrap-transit-response))

;; (def handler
;;   (api-middleware not-found-handler))
