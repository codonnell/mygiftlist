(ns rocks.mygiftlist.server-components.middleware
  (:require [mount.core :refer [defstate]]
            [rocks.mygiftlist.server-components.db :as db]
            [rocks.mygiftlist.server-components.pathom :as parser]
            [rocks.mygiftlist.config :as config]
            [com.fulcrologic.fulcro.server.api-middleware :refer [handle-api-request
                                                                  wrap-transit-params
                                                                  wrap-transit-response]]
            [ring.util.response :as resp]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.jwt :as jwt]
            [ring.middleware.gzip :as gzip]
            [taoensso.timbre :as log]
            [next.jdbc :as jdbc]))

(defn not-found-handler [_]
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

(defn wrap-healthcheck [handler uri pool]
  (fn [request]
    (if (= uri (:uri request))
      (try (jdbc/execute-one! pool ["SELECT 1 FROM \"user\""])
           {:status 200
            :body ""}
           (catch Throwable e
             (log/error (with-out-str (.printStackTrace e)))
             {:status 500
              :body ""}))
      (handler request))))

(defstate handler
  :start
  (-> not-found-handler
    (wrap-api "/api" db/pool)
    (jwt/wrap-jwt {:alg          :RS256
                   :jwk-endpoint config/jwk-endpoint})
    wrap-transit-params
    wrap-transit-response
    (wrap-defaults (assoc api-defaults :static {:resources "public"}))
    gzip/wrap-gzip
    (wrap-healthcheck "/healthcheck" db/pool)))
