(ns rocks.mygiftlist.ui.root
  (:require [rocks.mygiftlist.auth :as auth]
            [rocks.mygiftlist.routing :as routing]
            [rocks.mygiftlist.type.user :as user]
            [rocks.mygiftlist.ui.navigation :as ui.nav]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
            [com.fulcrologic.semantic-ui.elements.button.ui-button :refer [ui-button]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu :refer [ui-menu]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-menu :refer [ui-menu-menu]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-header :refer [ui-menu-header]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-item :refer [ui-menu-item]]
            [taoensso.timbre :as log]))

(defsc Home [this {:keys [left-nav]}]
  {:query [{:left-nav (comp/get-query ui.nav/LeftNav)}]
   :ident (fn [] [:component/id :home])
   :route-segment ["home"]
   :initial-state {:left-nav {}}}
  (dom/div :.home.container
    (ui.nav/ui-left-nav left-nav)
    (dom/div {}
      (dom/h3 "Home Screen"))))

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
           {:root/navbar (comp/get-query ui.nav/Navbar)}]
   :initial-state {:root/router {}
                   :root/navbar {}}}
  (dom/div {}
    (ui.nav/ui-navbar navbar)
    (ui-main-router router)))
