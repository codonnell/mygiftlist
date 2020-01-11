(ns rocks.mygiftlist.ui.gift-list
  (:require
   [rocks.mygiftlist.model.gift :as model.gift]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.ui.navigation :as ui.nav]

   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
   [com.fulcrologic.fulcro.mutations :as m]

   [com.fulcrologic.semantic-ui.elements.button.ui-button :refer [ui-button]]
   [com.fulcrologic.semantic-ui.collections.form.ui-form :refer [ui-form]]
   [com.fulcrologic.semantic-ui.collections.form.ui-form-field :refer [ui-form-field]]))

(defsc Gift [this {::gift/keys [id name]}]
  {:query [::gift/id ::gift/name]
   :ident ::gift/id}
  (dom/div {} (str "Gift id: " id ", name: " name)))

(def ui-gift (comp/factory Gift {:keyfn ::gift/id}))

(defsc GiftList [this {:ui/keys [gift-name] ::gift-list/keys [id name gifts]}]
  {:query [::gift-list/id ::gift-list/name :ui/gift-name
           {::gift-list/gifts (comp/get-query Gift)}]
   :ident ::gift-list/id
   :route-segment ["gift-list" ::gift-list/id]
   :will-enter (fn [app {::gift-list/keys [id]}]
                 (let [id (uuid id)]
                   ;; TODO: Handle canceling load if route is canceled?
                   (dr/route-deferred [::gift-list/id id]
                     #(df/load app [::gift-list/id id] GiftList
                        {:post-mutation `dr/target-ready
                         :post-mutation-params {:target [::gift-list/id id]}}))))}
  ;; TODO: Validate that the field isn't empty
  ;; TODO: Make this input not grow arbitrarily based on width of container div
  (dom/div {}
    (ui-form {:onSubmit (fn [evt]
                          (comp/transact! this [(model.gift/create-gift
                                                  #::gift {:id (random-uuid)
                                                           :name gift-name
                                                           :gift-list-id id})])
                          (m/set-string! this :ui/gift-name :value ""))}
      (ui-form-field {}
        (dom/input {:placeholder "A pony"
                    :onChange #(m/set-string! this :ui/gift-name :event %)
                    :value gift-name}))
      (ui-button {:type "submit"
                  :primary true}
        "Submit"))
    (mapv ui-gift gifts)))

(def ui-gift-list (comp/factory GiftList))
