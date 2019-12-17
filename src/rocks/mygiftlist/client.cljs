(ns rocks.mygiftlist.client
  (:require [rocks.mygiftlist.application :refer [SPA]]
            [rocks.mygiftlist.auth :as auth]
            [rocks.mygiftlist.config :as config]
            [rocks.mygiftlist.routing :as routing]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.semantic-ui.elements.header.ui-header :refer [ui-header]]
            [taoensso.timbre :as log]))

(defn auth-display []
  (dom/div {}
    (dom/button {:onClick #(auth/login)}
      "Login")
    (dom/button {:onClick #(auth/is-authenticated?)}
      "Check authenticated")))

(defsc Root [this _props]
  {:query []
   :initial-state {}}
  (dom/div {}
    (ui-header {:as "h1"} "Hello World")
    (dom/create-element auth-display)))

(defn ^:export refresh []
  (log/info "Hot code reload...")
  (auth/create-auth0-client!)
  (app/mount! SPA Root "app"))

(defn ^:export init []
  (log/info "Application starting...")
  (auth/create-auth0-client!)
  (routing/start!)
  (app/mount! SPA Root "app"))
