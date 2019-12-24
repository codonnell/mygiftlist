(ns rocks.mygiftlist.client
  (:require [rocks.mygiftlist.application :refer [SPA]]
            [rocks.mygiftlist.auth :as auth]
            [rocks.mygiftlist.config :as config]
            [rocks.mygiftlist.routing :as routing]
            [rocks.mygiftlist.model.user :as user]
            [clojure.core.async :refer [go]]
            [clojure.string :as str]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
            [com.fulcrologic.fulcro.ui-state-machines :as uism]
            [com.fulcrologic.semantic-ui.elements.button.ui-button :refer [ui-button]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu :refer [ui-menu]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-menu :refer [ui-menu-menu]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-header :refer [ui-menu-header]]
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
          (ui-menu-item {:active false
                         :onClick #(auth/login)}
            "Login/Signup"))))))

(def ui-navbar (comp/factory Navbar))

(defsc LeftNav [this _]
  {:query []
   :ident (fn [] [:component/id :left-nav])
   :initial-state {}}
  (ui-menu {:vertical true :className "leftnav menu"}
    (ui-menu-item {}
      (ui-menu-header {} "My Lists")
      (ui-menu-menu {}
        (ui-menu-item {:active false
                       :onClick #(js/console.log "Nav birthday 2019")}
          (dom/div "Birthday 2019"))
        (ui-menu-item {:active false
                       :onClick #(js/console.log "Nav christmas 2019")}
          (dom/div "Christmas 2019"))))
    (ui-menu-item {}
      (ui-menu-header {} "Shared Lists")
      (ui-menu-menu {}
        (ui-menu-item {:active false
                       :onClick #(js/console.log "Nav birthday 2019")}
          (dom/div "Birthday 2019")
          (dom/div "- Marty McFly"))
        (ui-menu-item {:active false
                       :onClick #(js/console.log "Nav christmas 2019")}
          (dom/div "Christmas 2019")
          (dom/div "- marty@example.com"))))))

(def ui-left-nav (comp/factory LeftNav))

(defsc User [this {::user/keys [id email auth0-id]}]
  {:query [::user/id ::user/email ::user/auth0-id]
   :ident ::user/id}
  (dom/div {}
    (str "ID: " id ", email: " email ", auth0 ID: " auth0-id)))

(def ui-user (comp/factory User))

(defsc AllUsers [this {:keys [all-users] :as props}]
  {:query [{:all-users (comp/get-query User)}]
   :ident (fn [] [:component/id :all-users])
   :initial-state {:all-users []}}
  (dom/div {}
    (mapv ui-user all-users)))

(def ui-all-users (comp/factory AllUsers))

(defsc Home [this {:keys [users current-user] :as props}]
  {:query [{:current-user (comp/get-query CurrentUser)}
           {:users (comp/get-query AllUsers)}]
   :ident (fn [] [:component/id :home])
   :route-segment ["home"]
   :initial-state {:current-user {}
                   :users {}}}
  (dom/div :.home.container
    (ui-left-nav)
    (dom/div {}
      (dom/h3 "Home Screen")
      (when (seq current-user)
        (ui-current-user current-user))
      (when (seq current-user)
        (ui-all-users users)))))

(defsc LoginForm [this _]
  {:query []
   :ident (fn [] [:component/id :login])
   :route-segment ["login"]
   :initial-state {}}
  (dom/div :.login.container
    (dom/div "In order to view and create gift lists, you need to...")
    (dom/div (ui-button {:primary true
                         :className "login combined-button"
                         :onClick #(auth/login)}
               "Log in or sign up"))))

(defrouter MainRouter [_ _] {:router-targets [LoginForm Home]})

(def ui-main-router (comp/factory MainRouter))

(defsc Root [this {:root/keys [router navbar] :as props}]
  {:query [{:root/router (comp/get-query MainRouter)}
           {:root/navbar (comp/get-query Navbar)}]
   :initial-state {:root/router {}
                   :root/navbar {}}}
  (dom/div {}
    (ui-navbar navbar)
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
    (when (str/includes? (.. js/window -location -search) "code=")
      (<! (auth/handle-redirect-callback)))
    (if-let [authenticated (<! (auth/is-authenticated?))]
      (let [{:strs [sub email]} (js->clj (<! (auth/get-user-info)))]
        (comp/transact! SPA [(auth/set-current-user {:user/id sub :user/email email})
                             (routing/route-to {:route-string "/home"})])
        (df/load! SPA [:component/id :all-users] AllUsers))
      (comp/transact! SPA [(routing/route-to {:route-string "/login"})])
      ;; Add current user info to app state and route to user home
      ;; Add anon user info to app state and route to anon home
      ;; NOTE: Should also get the bearer token from auth0-client as part of
      ;; request pipeline, don't store it in app state. Bearer token may change
      ;; because we're using authorization code flow.
      )
    ))

(comment
  (df/load! SPA [:component/id :all-users] AllUsers)
  )
