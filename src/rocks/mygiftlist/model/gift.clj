(ns rocks.mygiftlist.model.gift
  (:require
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [honeysql.helpers :as sqlh]
   [next.jdbc :as jdbc]
   [cognitect.anomalies :as anom]
   [rocks.mygiftlist.server-components.db :as db]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.gift-list.invitation :as invitation]
   [rocks.mygiftlist.type.gift-list.revocation :as revocation])
  (:import [org.postgresql.util PSQLException]))

(defn with-gift-access-control
  "Given a query selecting gift data with gift aliased as g, updates the query so
  that only gifts the requester has access to are returned.

  In particular, users should only be able to access gifts belonging to gift
  lists they have either created or accepted invitations to (and not had that
  invitation revoked)."
  [requester-auth0-id query]
  (sqlh/merge-join query
    [:gift_list_access :perm_gla] [:and
                                   [:= :perm_gla.auth0_id requester-auth0-id]
                                   [:= :perm_gla.gift_list_id :g.gift_list_id]]))

(defresolver gift-by-id-resolver [{::db/keys [pool] :keys [requester-auth0-id]} inputs]
  {::pc/input #{::gift/id}
   ::pc/output [::gift/name ::gift/description ::gift/url
                {::gift/claimed-by [::user/id]} ::gift/claimed-at
                {::gift/requested-by [::user/id]} ::gift/requested-at]
   ::pc/transform pc/transform-batch-resolver}
  (->> {:select [:g.id :g.name :g.description :g.url
                 :g.requested_by_id :g.requested_at
                 :g.claimed_by_id :g.claimed_at
                 :g.gift_list_id]
        :from [[:gift :g]]
        :where [:in :g.id (mapv ::gift/id inputs)]}
    (with-gift-access-control requester-auth0-id)
    (db/execute! pool)
    (mapv (fn [{::gift/keys [requested-by-id claimed-by-id gift-list-id] :as gift}]
            (-> gift
              (assoc-in [::gift/requested-by ::user/id] requested-by-id)
              (assoc-in [::gift/claimed-by ::user/id] claimed-by-id)
              (assoc-in [::gift/gift-list ::gift-list/id] gift-list-id)
              (dissoc ::gift/requested-by-id ::gift/claimed-by-id ::gift/gift-list-id))))
    (pc/batch-restore-sort {::pc/inputs inputs ::pc/key ::gift/id})))

(defmutation create-gift [{::db/keys [pool] :keys [requester-auth0-id]} {::gift/keys [gift-list-id] :as gift}]
  {::pc/input #{::gift/id ::gift/name ::gift/gift-list-id}
   ::pc/output [::gift/id]}
  (try
    (jdbc/with-transaction [tx pool {:isolation :serializable}]
      (when (seq (db/execute! tx {:select [1]
                                  :from [[:gift_list_access :gla]]
                                  :where [:and
                                          [:= :gla.auth0_id requester-auth0-id]
                                          [:= :gla.gift_list_id gift-list-id]]}))
        (db/execute! tx
          {:insert-into :gift
           :values [(assoc gift ::gift/requested-by-id {:select [:id]
                                                        :from [:user]
                                                        :where [:= :auth0_id requester-auth0-id]})]
           :returning [:id]})))
    (catch Throwable e
      (throw (ex-info "Error creating gift" {::anom/category ::anom/fault
                                             ::anom/message (.getMessage e)})))))

(def gift-resolvers
  [gift-by-id-resolver

   create-gift])

(comment
  (def requester-auth0-id "auth0|5dfeec6f9567eb0dc0302207")
  (def gift-list-id #uuid "dec2f711-7cac-4d91-bfad-0aeb0af85a52")
  (def gift #::gift {:gift-list-id gift-list-id
                     :id #uuid "e55e8bf0-aaff-4494-960d-2660609898aa"
                     :name "A pony"})
  (db/execute! db/pool {:select [1]
                        :from [[:gift_list_access :gla]]
                        :where [:and
                                [:= :gla.auth0_id requester-auth0-id]
                                [:= :gla.gift_list_id gift-list-id]]})
  (db/execute! db/pool
    {:insert-into :gift
     :values [(assoc gift ::gift/requested-by-id {:select [:id]
                                                  :from [:user]
                                                  :where [:= :auth0_id requester-auth0-id]})]
     :returning [:id]})
  )
