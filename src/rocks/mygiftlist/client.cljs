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
  (let [auth0 (auth/use-auth0)
        is-authenticated (.-isAuthenticated auth0)
        login-with-popup (.-loginWithPopup auth0)]
    (dom/div {}
      "Is authenticated:" (str is-authenticated)
      (dom/button {:onClick #(login-with-popup #js {})}
        "Login"))))

(defsc Root [this _props]
  {:query []
   :initial-state {}}
  (dom/create-element
    auth/auth0-provider
    (clj->js
      {:domain config/AUTH0_DOMAIN
       :client_id config/AUTH0_CLIENT_ID
       :redirect_uri (.. js/window -location -origin)
       :children [(ui-header {:as "h1"} "Hello World")
                  (dom/create-element auth-display)]})))

(defn ^:export refresh []
  (log/info "Hot code reload...")
  (app/mount! SPA Root "app"))

(defn ^:export init []
  (log/info "Application starting...")
  (routing/start!)
  (app/mount! SPA Root "app"))
