(ns user
  (:require
   ;; TODO: Include namespaces with mount components to properly start system
   [clojure.tools.namespace.repl :as tools-ns :refer [set-refresh-dirs]]
   [expound.alpha :as expound]
   [clojure.spec.alpha :as s]
   [mount.core :as mount]
   [mount.core :refer [defstate]]
   [clojure.pprint :refer [pprint]]
   [taoensso.timbre :as log]))

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
