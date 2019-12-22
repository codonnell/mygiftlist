(ns rocks.mygiftlist.client
  (:require [rocks.mygiftlist.application :refer [SPA]]
            [rocks.mygiftlist.auth :as auth]
            [rocks.mygiftlist.config :as config]
            [rocks.mygiftlist.routing :as routing]
            [clojure.core.async :refer [go]]
            [clojure.string :as str]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
            [com.fulcrologic.fulcro.ui-state-machines :as uism]
            [com.fulcrologic.semantic-ui.elements.header.ui-header :refer [ui-header]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu :refer [ui-menu]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-menu :refer [ui-menu-menu]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-item :refer [ui-menu-item]]
            [taoensso.timbre :as log]))

(defsc CurrentUser [this {:user/keys [id email]}]
  {:query [:user/id :user/email]
   :ident (fn [] [:component/id :current-user])
   :initial-state {}}
  (dom/div {}
    (dom/div {} (str "User id: " id))
    (dom/div {} (str "User email: " email))))

(def ui-current-user (comp/factory CurrentUser))

(defsc Navbar [this {:keys [current-user]}]
  {:query [{:current-user (comp/get-query CurrentUser)}]
   :ident (fn [] [:component/id :navbar])
   :initial-state {:current-user {}}}
  (let [logged-in (seq current-user)]
    (ui-menu {:secondary true}
      (ui-menu-item {:name "home"
                     :active false
                     :onClick #(comp/transact! this [(routing/route-to {:route-string (if logged-in "/home" "/login")})])})
      (ui-menu-menu {:position "right"}
        (if logged-in
          (ui-menu-item {:name "logout"
                         :active false
                         :onClick #(auth/logout)})
          (ui-menu-item {:name "login"
                         :active false
                         :onClick #(auth/login)}))))))

(def ui-navbar (comp/factory Navbar))

(defsc AuthDisplay [this _]
  (dom/div {}
    (dom/button {:onClick #(auth/login)}
      "Login")
    (dom/button {:onClick #(auth/is-authenticated?)}
      "Check authenticated")))

(def ui-auth-display (comp/factory AuthDisplay))

(defsc Home [this {:keys [current-user] :as props}]
  {:query [{:current-user (comp/get-query CurrentUser)}]
   :ident (fn [] [:component/id :home])
   :route-segment ["home"]
   :initial-state {:current-user {}}}
  (dom/div :.ui.container.segment
    (dom/h3 "Home Screen")
    (when (seq current-user)
      (ui-current-user current-user))))

(defsc LoginForm [this _]
  {:query []
   :ident (fn [] [:component/id :login])
   :route-segment ["login"]
   :initial-state {}}
  (dom/div :.ui.container.segment
    (dom/h3 "Login Screen")))

(defrouter MainRouter [_ _] {:router-targets [LoginForm Home]})

(def ui-main-router (comp/factory MainRouter))

(defsc Root [this {:root/keys [router navbar] :as props}]
  {:query [{:root/router (comp/get-query MainRouter)}
           {:root/navbar (comp/get-query Navbar)}]
   :initial-state {:root/router {}
                   :root/navbar {}}}
  (dom/div {}
    (ui-navbar navbar)
    (ui-header {:as "h1"} "Hello World")
    (ui-auth-display)
    (ui-main-router router)))

(defn ^:export refresh []
  (log/info "Hot code reload...")
  (app/mount! SPA Root "app"))

(defn spy [x]
  (println x)
  x)

(defn ^:export init []
  (log/info "Application starting...")
  (go
    (app/mount! SPA Root "app")
    (routing/start!)
    (<! (auth/create-auth0-client!))
    (when (str/includes? (.. js/window -location -search) "code=")
      (<! (auth/handle-redirect-callback)))
    (if-let [authenticated (<! (auth/is-authenticated?))]
      (let [{:strs [sub email]} (spy (js->clj (<! (auth/get-user-info))))]
        (comp/transact! SPA [(auth/set-current-user {:user/id sub :user/email email})
                             (routing/route-to {:route-string "/home"})]))
      (comp/transact! SPA [(routing/route-to {:route-string "/login"})])
      ;; Add current user info to app state and route to user home
      ;; Add anon user info to app state and route to anon home
      ;; NOTE: Should also get the bearer token from auth0-client as part of
      ;; request pipeline, don't store it in app state. Bearer token may change
      ;; because we're using authorization code flow.
      )
    ))
