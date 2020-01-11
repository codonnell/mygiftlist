(ns rocks.mygiftlist.ui.root
  (:require [rocks.mygiftlist.auth :as auth]
            [rocks.mygiftlist.model.gift-list :as model.gift-list]
            [rocks.mygiftlist.routing :as routing]
            [rocks.mygiftlist.type.gift-list :as gift-list]
            [rocks.mygiftlist.type.user :as user]
            [rocks.mygiftlist.ui.navigation :as ui.nav]
            [rocks.mygiftlist.ui.gift-list :as ui.gift-list]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.mutations :as m]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
            [com.fulcrologic.semantic-ui.elements.button.ui-button :refer [ui-button]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu :refer [ui-menu]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-menu :refer [ui-menu-menu]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-header :refer [ui-menu-header]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-item :refer [ui-menu-item]]

            [com.fulcrologic.semantic-ui.collections.form.ui-form :refer [ui-form]]
            [com.fulcrologic.semantic-ui.collections.form.ui-form-field :refer [ui-form-field]]
            [taoensso.timbre :as log]))

(defsc Home [this {:ui/keys [gift-list-name]}]
  {:query [:ui/gift-list-name]
   :ident (fn [] [:component/id :home])
   :route-segment ["home"]
   :initial-state {:ui/gift-list-name ""}}
  (dom/div {}
    (dom/h3 "Home Screen")
    (dom/div "Just getting started? Create a new gift list!")
    ;; TODO: Validate that the field isn't empty
    ;; TODO: Make this input not grow arbitrarily based on width of container div
    (ui-form {:onSubmit (fn [evt]
                          (let [id (random-uuid)]
                            (comp/transact! this [(model.gift-list/create-gift-list
                                                    #::gift-list {:id id
                                                                  :name gift-list-name})])
                            (m/set-string! this :ui/gift-list-name :value "")
                            (comp/transact! this [(routing/route-to
                                                    {:route-string (str "/gift-list/" id)})])))}
      (ui-form-field {}
        (dom/input {:placeholder "Birthday 2020"
                    :onChange #(m/set-string! this :ui/gift-list-name :event %)
                    :value gift-list-name}))
      (ui-button {:type "submit"
                  :primary true}
        "Submit"))))

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

(defrouter MainRouter [_ _] {:router-targets [LoginForm Home ui.gift-list/GiftList]})

(def ui-main-router (comp/factory MainRouter))

(defsc Root [this {:root/keys [router navbar left-nav] :as props}]
  {:query [{:root/router (comp/get-query MainRouter)}
           {:root/navbar (comp/get-query ui.nav/Navbar)}
           {:root/left-nav (comp/get-query ui.nav/LeftNav)}]
   :initial-state {:root/router {}
                   :root/navbar {}
                   :root/left-nav {}}}
  (dom/div {}
    (ui.nav/ui-navbar navbar)
    (dom/div :.home.container
      (ui.nav/ui-left-nav left-nav)
      (ui-main-router router))))
