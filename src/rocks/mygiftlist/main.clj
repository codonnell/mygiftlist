(ns rocks.mygiftlist.main
  (:require rocks.mygiftlist.server-components.http-server
            [mount.core :as mount]))

(defn -main [& args]
  (mount/start))
