(ns rocks.mygiftlist.model.gift-list
  (:require
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.gift-list.invitation :as invitation]
   [rocks.mygiftlist.type.gift-list.revocation :as revocation]))

(defmutation create-gift-list [{::gift-list/keys [id] :as gift-list}]
  (action [{:keys [state]}]
    (let [current-user-id (get-in @state [:component/id :current-user ::user/id])
          gift-list (assoc gift-list ::created-by [::user/id current-user-id])]
      (swap! state
        #(-> %
           (assoc-in [::gift-list/id id] gift-list)
           (update-in [:component/id :left-nav :created-gift-lists] conj [::gift-list/id id])))))
  (remote [_] true))
