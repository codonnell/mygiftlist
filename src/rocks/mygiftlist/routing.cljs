(ns rocks.mygiftlist.routing
  (:require [rocks.mygiftlist.application :refer [SPA]]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [clojure.string :as str]
            [pushy.core :as pushy]))

(defonce history (pushy/pushy
                   (fn [path]
                     (let [route-segments (into [] (drop 1) (str/split (first (str/split path "?")) "/"))]
                       (dr/change-route SPA route-segments)))
                   identity))

(defn start! []
  (pushy/start! history))

(defn route-to!
  "Change routes to the given route-string (e.g. \"/home\")."
  [route-string]
  (pushy/set-token! history route-string))

(defmutation route-to
  "Mutation to go to a specific route"
  [{:keys [route-string]}]
  (action [_]
    (route-to! route-string)))
