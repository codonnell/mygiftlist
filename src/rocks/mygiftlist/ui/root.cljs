(ns rocks.mygiftlist.ui.root
  (:require [rocks.mygiftlist.auth :as auth]
            [rocks.mygiftlist.model.gift-list :as model.gift-list]
            [rocks.mygiftlist.routing :as routing]
            [rocks.mygiftlist.type.gift-list :as gift-list]
            [rocks.mygiftlist.type.user :as user]
            [rocks.mygiftlist.ui.navigation :as ui.nav]
            [rocks.mygiftlist.ui.gift-list :as ui.gift-list]

            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.algorithms.merge :as merge]
            [com.fulcrologic.fulcro.application :as app]
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
            [com.fulcrologic.semantic-ui.collections.form.ui-form-input :refer [ui-form-input]]
            [taoensso.timbre :as log]))

(declare Home)

(defsc GiftListForm [this {::gift-list/keys [id name] :as gift-list}]
  {:query [::gift-list/id ::gift-list/name fs/form-config-join]
   :ident ::gift-list/id
   :form-fields #{::gift-list/name}}
  (let [validity (fs/get-spec-validity gift-list ::gift-list/name)]
    (dom/div {}
      (ui-form {:onSubmit (fn [evt]
                            (if-not (= :valid validity)
                              (comp/transact! this [(fs/mark-complete! {})])
                              (do
                                (comp/transact! this [(model.gift-list/create-gift-list
                                                        (select-keys gift-list
                                                          [::gift-list/id ::gift-list/name]))])
                                (merge/merge-component! this Home
                                  {:ui/gift-list-form (fs/add-form-config
                                                        GiftListForm
                                                        {::gift-list/id (random-uuid)
                                                         ::gift-list/name ""})}))))}
        (ui-form-input {:placeholder "Birthday 2020"
                        :className "mgl_text-input"
                        :onChange (fn [evt]
                                    (m/set-string! this ::gift-list/name :event evt)
                                    (comp/transact! this [(fs/mark-complete! {:field ::gift-list/name})]))
                        :error (and (= :invalid validity) "Gift list name cannot be blank")
                        :fluid true
                        :value name})
        (ui-button {:type "submit"
                    :primary true
                    :disabled (= :invalid validity)}
          "Submit")))))

(def ui-gift-list-form (comp/factory GiftListForm))

(defsc Loading [this _]
  {:query []
   :ident (fn [] [:component/id ::loading])
   :initial-state {}
   :route-segment ["loading"]}
  (dom/div "Loading..."))

(defsc Home [this {:ui/keys [gift-list-form] :as props}]
  {:query [{:ui/gift-list-form (comp/get-query GiftListForm)}]
   :ident (fn [] [:component/id :home])
   :initial-state {}
   :route-segment ["home"]
   :will-enter (fn [{::app/keys [state-atom] :as app} _]
                 (when-not (get-in @state-atom [:component/id :home :ui/gift-list-form])
                   (merge/merge-component! app Home
                     {:ui/gift-list-form (fs/add-form-config
                                           GiftListForm
                                           {::gift-list/id (random-uuid)
                                            ::gift-list/name ""})}))
                 (dr/route-immediate [:component/id :home]))}
  (dom/div {}
    (dom/h3 "Home Screen")
    (dom/div "Just getting started? Create a new gift list!")
    (ui-gift-list-form gift-list-form)))

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

(defrouter MainRouter [_ {:keys [current-state] :as props}]
  {:router-targets [Loading LoginForm Home ui.gift-list/GiftList]}
  (case current-state
    :pending (dom/div "Pending...")
    (dom/div "Loading...")))

(def ui-main-router (comp/factory MainRouter))

(defsc FlashMessage [this {:ui/keys [message type active]}]
  {:query [:ui/active :ui/message :ui/type]
   :ident (fn [] [:component/id :flash-message])
   :initial-state {:ui/active false}}
  (when active
    (dom/div :.ui.message.mgl_flash-message {:className (name type)}
      (dom/p message))))

(def ui-flash-message (comp/factory FlashMessage))

(defsc Root [this {:root/keys [router flash-message navbar left-nav] :as props}]
  {:query [{:root/router (comp/get-query MainRouter)}
           {:root/flash-message (comp/get-query FlashMessage)}
           {:root/navbar (comp/get-query ui.nav/Navbar)}
           {:root/left-nav (comp/get-query ui.nav/LeftNav)}]
   :initial-state {:root/router {}
                   :root/flash-message {}
                   :root/navbar {}
                   :root/left-nav {}}}
  (dom/div {}
    (ui.nav/ui-navbar navbar)
    (ui-flash-message flash-message)
    (dom/div :.home.container
      (ui.nav/ui-left-nav left-nav)
      (ui-main-router router))))
