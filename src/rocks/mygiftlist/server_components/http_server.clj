(ns rocks.mygiftlist.server-components.http-server
  (:require [mount.core :refer [defstate]]
            [org.httpkit.server :as http-kit]
            [rocks.mygiftlist.server-components.middleware :as middleware]))

(defstate http-server
  :start
  ;; TODO: Make port configurable
  (http-kit/run-server middleware/handler {:port 8080})
  :stop (http-server))
