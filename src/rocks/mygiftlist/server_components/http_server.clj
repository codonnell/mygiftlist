(ns rocks.mygiftlist.server-components.http-server
  (:require [mount.core :refer [defstate]]
            [org.httpkit.server :as http-kit]
            [rocks.mygiftlist.config :as config]
            [rocks.mygiftlist.server-components.middleware :as middleware]))

(defstate http-server
  :start
  (http-kit/run-server middleware/handler {:port config/port})
  :stop (http-server))
