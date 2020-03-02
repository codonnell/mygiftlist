(ns rocks.mygiftlist.model.gift-list.invitation
  (:require
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]
   [com.fulcrologic.fulcro.algorithms.normalized-state :refer [swap!->]]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [com.wsscode.pathom.core :as p]
   [edn-query-language.core :as eql]
   [cognitect.anomalies :as anom]

   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.gift-list.invitation :as invitation]
   [rocks.mygiftlist.type.gift-list.revocation :as revocation]))

(defmutation create-invitation [{::gift-list/keys [id] :as invitation}]
  (ok-action [{:keys [state result]}]
    (let [invitation (merge invitation (get-in result [:body `create-invitation]))
          invitation-id (::invitation/id invitation)]
      ;; TODO: Make invitation show in UI
      (swap!-> state
        (assoc-in [::invitation/id invitation-id] invitation)
        (assoc-in [::gift-list/id id ::gift-list/invitation] [::invitation/id invitation-id]))))
  (error-action [{:keys [state result] :as env}]
    (let [message (get-in env [:result :body `create-invitation ::p/error ::anom/message])]
      (swap! state assoc-in [:component/id :flash-message] #:ui{:active true
                                                                :message message
                                                                :type "negative"})))
  (remote [_] true))
