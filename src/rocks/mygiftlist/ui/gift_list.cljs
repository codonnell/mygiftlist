(ns rocks.mygiftlist.ui.gift-list
  (:require
   [rocks.mygiftlist.model.gift :as model.gift]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.ui.navigation :as ui.nav]

   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
   [com.fulcrologic.fulcro.mutations :as m]

   [com.fulcrologic.semantic-ui.elements.button.ui-button :refer [ui-button]]
   [com.fulcrologic.semantic-ui.collections.form.ui-form :refer [ui-form]]
   [com.fulcrologic.semantic-ui.collections.form.ui-form-field :refer [ui-form-field]]
   [com.fulcrologic.semantic-ui.collections.form.ui-form-input :refer [ui-form-input]]
   ))

(defsc GiftForm [this {::gift/keys [name] :as gift}]
  {:query [::gift/id ::gift/name fs/form-config-join]
   :ident ::gift/id
   :form-fields #{::gift/name}}
  (dom/div {}
    (ui-form {:onSubmit (fn [evt]
                          (comp/transact! this [(model.gift/create-gift gift)])
                          (m/set-string! this :ui/gift-name :value ""))}
      (ui-form-field {}
        (ui-form-input (cond-> {:placeholder "A pony"
                                :onChange #(m/set-string! this ::gift/name :event %)
                                :onBlur #(comp/transact! this [(fs/mark-complete! {:field ::gift/name})])
                                :fluid true
                                :value name}
                         (fs/invalid-spec? gift ::gift/name)
                         (assoc :error "Gift names cannot be blank"))))
      (ui-button {:type "submit"
                  :primary true}
        "Submit"))))

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
   :will-enter (fn [app {::gift-list/keys [id]}]
                 (let [id (uuid id)]
                   ;; TODO: Handle canceling load if route is canceled?
                   (dr/route-deferred [::gift-list/id id]
                     (fn []
                       ;; TODO: Don't do this merge if gift already exists for this form
                       (merge/merge-component! app GiftList
                         {::gift-list/id id
                          :ui/gift-form (fs/add-form-config
                                          GiftForm
                                          {::gift/id (random-uuid)
                                           ::gift/name ""})})
                       (df/load app [::gift-list/id id] GiftList
                         {:post-mutation `dr/target-ready
                          :post-mutation-params {:target [::gift-list/id id]}})))))}
  ;; TODO: Validate that the field isn't empty
  ;; TODO: Make this input not grow arbitrarily based on width of container div
  (dom/div {}
    (ui-gift-form gift-form)
    (mapv ui-gift gifts)))

(def ui-gift-list (comp/factory GiftList))
