(ns rocks.mygiftlist.application
  (:require [com.fulcrologic.fulcro.application :as app]
            [rocks.mygiftlist.http-remote :as http-remote]
            [rocks.mygiftlist.auth :as auth]
            [com.fulcrologic.fulcro.rendering.keyframe-render2 :as keyframe-render2]))

(defonce SPA (app/fulcro-app {:optimized-render! keyframe-render2/render!
                              :remotes {:remote (http-remote/fulcro-http-remote {})}}))
