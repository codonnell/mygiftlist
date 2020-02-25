(ns rocks.mygiftlist.ui.gift-list
  (:require
   [rocks.mygiftlist.model.gift :as model.gift]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.ui.navigation :as ui.nav]

   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
   [com.fulcrologic.fulcro.mutations :as m]

   [com.fulcrologic.semantic-ui.elements.button.ui-button :refer [ui-button]]
   [com.fulcrologic.semantic-ui.collections.form.ui-form :refer [ui-form]]
   [com.fulcrologic.semantic-ui.collections.form.ui-form-input :refer [ui-form-input]]
   ))

(declare GiftList)

(defsc GiftForm [this {::gift/keys [name gift-list-id] :ui/keys [submitting] :as gift}]
  {:query [::gift/id ::gift/name ::gift/gift-list-id fs/form-config-join :ui/submitting]
   :ident ::gift/id
   :form-fields #{::gift/name}}
  (let [validity (fs/get-spec-validity gift ::gift/name)]
    (dom/div {}
      (ui-form {:onSubmit (fn [evt]
                            (if-not (= :valid validity)
                              (comp/transact! this [(fs/mark-complete! {})])
                              (comp/transact! this [(model.gift/create-gift
                                                      (assoc (select-keys gift
                                                               [::gift/id ::gift/name ::gift/gift-list-id])
                                                        :ui/wrapper-class GiftList
                                                        :ui/form-class GiftForm))])))}
        (ui-form-input {:placeholder "A pony"
                        :className "mgl_text-input"
                        :onChange (fn [evt]
                                    (m/set-string! this ::gift/name :event evt)
                                    (comp/transact! this [(fs/mark-complete! {:field ::gift/name})]))
                        :error (and (= :invalid validity) "Gift name cannot be blank")
                        :fluid true
                        :value name})
        (ui-button {:type "submit"
                    :primary true
                    :loading submitting
                    :disabled (= :invalid validity)}
          "Submit")))))

(def ui-gift-form (comp/factory GiftForm))

(defsc Gift [this {::gift/keys [id name]}]
  {:query [::gift/id ::gift/name]
   :ident ::gift/id}
  (dom/div {} (str "Gift id: " id ", name: " name)))

(def ui-gift (comp/factory Gift {:keyfn ::gift/id}))

(defsc GiftList [this {:ui/keys [gift-form] ::gift-list/keys [id name gifts]}]
  {:query [::gift-list/id ::gift-list/name
           {:ui/gift-form (comp/get-query GiftForm)}
           {::gift-list/gifts (comp/get-query Gift)}]
   :ident ::gift-list/id
   :route-segment ["gift-list" ::gift-list/id]
   :will-enter (fn [{::app/keys [state-atom] :as app} {::gift-list/keys [id]}]
                 (let [id (uuid id)]
                   (dr/route-deferred [::gift-list/id id]
                     (fn []
                       (merge/merge-component! app GiftList
                         (cond-> {::gift-list/id id}
                           (not (get-in @state-atom [::gift-list/id id :ui/gift-form]))
                           (assoc :ui/gift-form (fs/add-form-config
                                                  GiftForm
                                                  {::gift/id (random-uuid)
                                                   ::gift/name ""
                                                   ::gift/gift-list-id id
                                                   :ui/submitting false}))))
                       (df/load app [::gift-list/id id] GiftList
                         {:post-mutation `dr/target-ready
                          :post-mutation-params {:target [::gift-list/id id]}})))))}
  (dom/div {}
    (dom/h3 {} name)
    (ui-gift-form gift-form)
    (mapv ui-gift gifts)))

(def ui-gift-list (comp/factory GiftList))
