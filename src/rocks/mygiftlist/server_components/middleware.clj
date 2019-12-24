(ns rocks.mygiftlist.server-components.middleware
  (:require
   [rocks.mygiftlist.server-components.pathom :refer [parser]]
   [com.fulcrologic.fulcro.server.api-middleware :refer [handle-api-request
                                                         wrap-transit-params
                                                         wrap-transit-response]]
   [taoensso.timbre :as log]))

;; TODO: Respond here with index.html
(def ^:private not-found-handler
  (fn [req]
    {:status  404
     :headers {"Content-Type" "text/plain"}
     :body    "Not found"}))

(defn wrap-api [handler uri]
  (fn [request]
    (if (= uri (:uri request))
      #_{:status 200
       :headers {"Content-Type" "text/plain"}
       :body "Success!"}
      (handle-api-request
        (:transit-params request)
        (fn [tx] (parser {:ring/request request} tx)))
      (handler request))))

(defn api-middleware [handler]
  (-> handler
    (wrap-api "/api")
    wrap-transit-params
    wrap-transit-response))

(def handler
  (api-middleware not-found-handler))
