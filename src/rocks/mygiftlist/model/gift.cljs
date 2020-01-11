(ns rocks.mygiftlist.model.gift
  (:require
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.gift-list.invitation :as invitation]
   [rocks.mygiftlist.type.gift-list.revocation :as revocation]))

;; TODO: Handle requested-at
(defmutation create-gift [{::gift/keys [id gift-list-id] :as gift}]
  (action [{:keys [state]}]
    (let [current-user-id (get-in @state [:component/id :current-user ::user/id])
          gift (assoc gift ::requested-by [::user/id current-user-id])]
      (swap! state
        #(-> %
           (assoc-in [::gift/id id] gift)
           (update-in [::gift-list/id gift-list-id ::gift-list/gifts] conj gift)))))
  (remote [_] true))
