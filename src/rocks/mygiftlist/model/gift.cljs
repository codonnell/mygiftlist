(ns rocks.mygiftlist.model.gift
  (:require
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [com.wsscode.pathom.core :as p]
   [edn-query-language.core :as eql]
   [cognitect.anomalies :as anom]

   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.gift-list.invitation :as invitation]
   [rocks.mygiftlist.type.gift-list.revocation :as revocation]))

(defmutation create-gift [{:ui/keys [wrapper-class form-class] ::gift/keys [id gift-list-id] :as gift}]
  (action [{:keys [state]}]
    (swap! state assoc-in [::gift/id id :ui/submitting] true))
  (ok-action [{:keys [state]}]
    (let [current-user-id (get-in @state [:component/id :current-user ::user/id])
          gift (-> gift
                 (assoc ::requested-by [::user/id current-user-id])
                 (dissoc :ui/wrapper-class :ui/form-class))]
      (swap! state
        #(-> %
           (assoc-in [::gift/id id] gift)
           (update-in [::gift-list/id gift-list-id ::gift-list/gifts] conj gift)
           (merge/merge-component wrapper-class
             {::gift-list/id gift-list-id
              :ui/gift-form (fs/add-form-config
                              form-class
                              {::gift/id (random-uuid)
                               ::gift/name ""
                               ::gift/gift-list-id gift-list-id
                               :ui/submitting false})})))))
  (error-action [{:keys [state result] :as env}]
    (let [message (get-in env [:result :body `create-gift ::p/error ::anom/message])]
      (swap! state
        #(-> %
           (assoc-in [::gift/id id :ui/submitting] false)
           (assoc-in [:component/id :flash-message] #:ui{:active true
                                                         :message message
                                                         :type "negative"})))))
  (remote [_] (eql/query->ast1 [(create-gift (dissoc gift :ui/wrapper-class :ui/form-class))])))
