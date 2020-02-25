(ns rocks.mygiftlist.server-components.middleware
  (:require [mount.core :refer [defstate]]
            [rocks.mygiftlist.type.error :as error]
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
            [com.wsscode.pathom.core :as p]
            [next.jdbc :as jdbc]))

(defn not-found-handler [_]
  (assoc-in (resp/resource-response "public/index.html")
    [:headers "Content-Type"] "text/html"))

(defn wrap-api [handler uri pool]
  (fn [request]
    (if (= uri (:uri request))
      (let [result (parser/parser {:ring/request request}
                     (:transit-params request))
            errors (sequence (keep (comp ::p/error val)) result)]
        (log/debug "result" result)
        (assoc-in {:status (if (seq errors) 500 200) :body result}
          [:headers "Content-Type"] "application/transit+json"))
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
