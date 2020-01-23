(ns rocks.mygiftlist.routing
  (:require [rocks.mygiftlist.application :refer [SPA]]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [clojure.string :as str]
            [goog.object :as gobj]
            [pushy.core :as pushy]))

(defonce page-loaded (atom false))

(defn path->route-segments [path]
  (let [route-segments (into [] (drop 1) (str/split (first (str/split path "?")) "/"))]
    (if (seq route-segments)
      (dr/change-route SPA route-segments)
      (dr/change-route SPA ["home"]))))

(defonce history (pushy/pushy
                   (fn [path]
                     (dr/change-route SPA (path->route-segments path)))
                   (fn [uri]
                     (if-not @page-loaded
                       (do (reset! page-loaded true)
                           false)
                       uri))))

(defn- get-current-path []
  (str (.-pathname js/location) (.-search js/location) (.-hash js/location)))

(defn start! []
  (pushy/start! history))

(defn route-to!
  "Change routes to the given path (e.g. \"/home\")."
  [path]
  (if (= path (get-current-path))
    (dr/change-route SPA (path->route-segments path))
    (pushy/set-token! history path)))

(defn save! []
  (gobj/set js/localStorage "saved-path" (let [path (get-current-path)]
                                           (if (= path "/login") "/home" path))))

(defn restore! []
  (let [path (gobj/get js/localStorage "saved-path")]
    (gobj/remove js/localStorage "saved-path")
    (route-to! (or path (get-current-path)))))

(defmutation route-to
  "Mutation to go to a specific route"
  [{:keys [path]}]
  (action [_]
    (route-to! path)))
