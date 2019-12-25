(ns user
  (:require
   [clojure.tools.namespace.repl :as tools-ns :refer [set-refresh-dirs]]
   [expound.alpha :as expound]
   [clojure.spec.alpha :as s]
   [mount.core :as mount]
   [rocks.mygiftlist.server-components.middleware :as middleware]
   [rocks.mygiftlist.config :as config]
   [mount.core :refer [defstate]]
   [clojure.pprint :refer [pprint]]
   [org.httpkit.server :as http-kit]
   [ring.util.response :as resp]
   [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
   [taoensso.timbre :as log]))

;; ==================== CONFIG ====================

(alter-var-root #'config/environment (constantly :dev))

(defmethod config/get-config :dev [_]
  {:db-name "my-db"
   :jwk-endpoint "https://mygiftlistrocks-dev.auth0.com/.well-known/jwks.json"})

;; ==================== SERVER ====================

(defn not-found-handler [req]
  (assoc-in (resp/resource-response "public/index.html")
    [:headers "Content-Type"] "text/html"))

(def middleware
  (-> not-found-handler
    middleware/api-middleware
    (wrap-defaults (assoc api-defaults :static {:resources "public"}))))

(defstate http-server
  :start
  (http-kit/run-server middleware {:port 8080})
  :stop (http-server))

;; ==================== REPL TOOLING ====================

(set-refresh-dirs "src" "dev")
;; Change the default output of spec to be more readable
(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start
  "Start the web server"
  [] (mount/start))

(defn stop
  "Stop the web server"
  [] (mount/stop))

(defn restart
  "Stop, reload code, and restart the server. If there is a compile error, use:
  ```
  (tools-ns/refresh)
  ```
  to recompile, and then use `start` once things are good."
  []
  (stop)
  (tools-ns/refresh :after 'user/start))
