(ns rocks.mygiftlist.client
  (:require [rocks.mygiftlist.application :refer [SPA]]
            [rocks.mygiftlist.auth :as auth]
            [rocks.mygiftlist.config :as config]
            [rocks.mygiftlist.routing :as routing]
            [clojure.core.async :refer [go]]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
            [com.fulcrologic.semantic-ui.elements.header.ui-header :refer [ui-header]]
            [taoensso.timbre :as log]))

(defsc AuthDisplay [this _]
  (dom/div {}
    (dom/button {:onClick #(auth/login)}
      "Login")
    (dom/button {:onClick #(auth/is-authenticated?)}
      "Check authenticated")))

(def ui-auth-display (comp/factory AuthDisplay))

(defsc Home [this _]
  {:query []
   :ident (fn [] [:component/id :home])
   :route-segment ["home"]
   :initial-state {}}
  (dom/div :.ui.container.segment
    (dom/h3 "Home Screen")))

(defsc LoginForm [this _]
  {:query []
   :ident (fn [] [:component/id :login])
   :route-segment ["login"]
   :initial-state {}}
  (dom/div :.ui.container.segment
    (dom/h3 "Login Screen")))

(defrouter MainRouter [_ _] {:router-targets [LoginForm Home]})

(def ui-main-router (comp/factory MainRouter))

(defsc Root [this {:root/keys [router]}]
  {:query [{:root/router (comp/get-query MainRouter)}]
   :initial-state {:root/router {}}}
  (dom/div {}
    (ui-header {:as "h1"} "Hello World")
    (ui-auth-display)
    (ui-main-router router)))

(defn ^:export refresh []
  (log/info "Hot code reload...")
  (app/mount! SPA Root "app"))

(defn ^:export init []
  (log/info "Application starting...")
  (go
    (app/mount! SPA Root "app")
    (routing/start!)
    (<! (auth/create-auth0-client!))
    (if-let [authenticated (<! (auth/is-authenticated?))]
      (comp/transact! SPA [(routing/route-to {:route-string "/home"})])
      (comp/transact! SPA [(routing/route-to {:route-string "/login"})])
      ;; Add current user info to app state and route to user home
      ;; Add anon user info to app state and route to anon home
      ;; NOTE: Should also get the bearer token from auth0-client as part of
      ;; request pipeline, don't store it in app state. Bearer token may change
      ;; because we're using authorization code flow.
      )
    ))
