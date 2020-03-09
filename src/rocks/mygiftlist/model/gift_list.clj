(ns rocks.mygiftlist.model.gift-list
  (:require
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [next.jdbc :as jdbc]
   [honeysql.core :as sql]
   [honeysql.helpers :as sqlh]
   [rocks.mygiftlist.server-components.db :as db]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.gift-list.invitation :as invitation]
   [rocks.mygiftlist.type.gift-list.revocation :as revocation]
   [taoensso.timbre :as log]))

(defn with-gift-list-access-control
  "Given a query selecting gift list data with gift list aliased as gl, updates
  the query so that only gift lists the requester has access to are returned.

  In particular, users should only be able to access gift lists they have either
  created or accepted invitations to (and not had that invitation revoked)."
  [requester-auth0-id query]
  (sqlh/merge-join query
    [:gift_list_access :perm_gla] [:and
                                   [:= :perm_gla.auth0_id requester-auth0-id]
                                   [:= :perm_gla.gift_list_id :gl.id]]))

(defresolver gift-list-by-id-resolver [{::db/keys [pool] :keys [requester-auth0-id]} inputs]
  {::pc/input #{::gift-list/id}
   ::pc/output [::gift-list/name ::gift-list/created-at
                {::gift-list/created-by [::user/id]}
                {::gift-list/gifts [::gift/id]}]
   ::pc/transform pc/transform-batch-resolver}
  (->> {:select [:gl.id :gl.name :gl.created_at :u.id
                 [(sql/call :array_agg :g.id) :gift_ids]]
        :modifiers [:distinct]
        :from [[:gift_list :gl]]
        :left-join [[:gift :g] [:= :g.gift_list_id :gl.id]
                    [:user :u] [:= :u.id :gl.created_by_id]]
        :where [:in :gl.id (mapv ::gift-list/id inputs)]
        :group-by [:gl.id :gl.name :gl.created_at :gl.created_by_id :u.id]}
    (with-gift-list-access-control requester-auth0-id)
    (db/execute! pool)
    (mapv (fn [{:keys [gift-ids] :as gift-list}]
            (-> gift-list
              (assoc ::gift-list/gifts
                (into [] (keep #(when % (hash-map ::gift/id %))) gift-ids))
              (assoc-in [::gift-list/created-by ::user/id] (::user/id gift-list))
              (dissoc :gift-ids ::user/id))))
    (pc/batch-restore-sort {::pc/inputs inputs ::pc/key ::gift-list/id})))

(defresolver created-gift-lists-resolver [{::db/keys [pool] :keys [requester-auth0-id]} _]
  {::pc/output [{:created-gift-lists [::gift-list/id]}]}
  {:created-gift-lists
   (db/execute! pool
     {:select [:gl.id]
      :from [[:gift_list :gl]]
      :join [[:user :u] [:= :u.id :gl.created_by_id]]
      :where [:= :u.auth0_id requester-auth0-id]
      :order-by [[:gl.created_at :desc]]})})

;; TODO: Don't return duplicates of the same gift list
(defresolver invited-gift-lists-resolver [{::db/keys [pool] :keys [requester-auth0-id]} _]
  {::pc/output [{:invited-gift-lists [::gift-list/id]}]}
  {:invited-gift-lists
   (db/execute! pool
     {:select [:gl.id]
      :from [[:gift_list :gl]]
      :join [[:invitation :i] [:= :i.gift_list_id :gl.id]
             [:invitation_acceptance :ia] [:= :ia.invitation_id :i.id]
             [:user :u] [:= :u.id :ia.accepted_by_id]]
      :left-join [[:revocation :r] [:and
                                    [:= :r.revoked_user_id :u.id]
                                    [:= :r.gift_list_id :gl.id]]]
      :where [:and
              [:= :u.auth0_id requester-auth0-id]
              [:= :r.id nil]]
      :order-by [[:gl.created_at :desc]]})})

(defmutation create-gift-list [{::db/keys [pool] :keys [requester-auth0-id]} {::gift-list/keys [id name]}]
  {::pc/params #{::gift-list/id ::gift-list/name}
   ::pc/output [::gift-list/id]}
  (db/execute-one! pool
    {:insert-into :gift_list
     :values [{:id id
               :name name
               :created_by_id {:select [:id]
                               :from [:user]
                               :where [:= :auth0_id requester-auth0-id]}}]
     :returning [:id]}))

(def gift-list-resolvers
  [gift-list-by-id-resolver
   created-gift-lists-resolver
   invited-gift-lists-resolver

   create-gift-list])

(comment
  (mount.core/start)
  (let [ids [#uuid "df687d54-c716-4fcc-9f88-03f4fee90209"]
        raw-results (jdbc/execute! db/pool
                      (sql/format
                        {:select [:gl.id :gl.name :gl.created_at
                                  [(sql/call :array_agg :u.id) :created_by_ids]
                                  [(sql/call :array_agg :g.id) :gift_ids]]
                         :from [[:gift_list :gl]]
                         :left-join [[:gift :g] [:= :g.gift_list_id :gl.id]
                                     [:user :u] [:= :u.id :gl.created_by_id]]
                         :where [:in :gl.id [#uuid "df687d54-c716-4fcc-9f88-03f4fee90209"]]
                         :group-by [:gl.id :gl.name :gl.created_at :gl.created_by_id]}
                        :quoting :ansi)
                      db/query-opts)]
    raw-results)
  (def requester-auth0-id "auth0|5dfeec6f9567eb0dc0302207")
  (def pool db/pool)
  (sql/format {:select [:gl.id]
               :modifiers [:distinct]
               :from [[:gift_list :gl]]
               :join [[:invitation :i] [:= :i.gift_list_id :gl.id]
                      [:invitation_acceptance :ia] [:= :ia.invitation_id :i.id]
                      [:user :u] [:= :u.id :ia.accepted_by_id]]
               :left-join [[:revocation :r] [:and
                                             [:= :r.revoked_user_id :u.id]
                                             [:= :r.gift_list_id :gl.id]]]
               :where [:and
                       [:= :u.auth0_id "auth0|1234"]
                       [:= :r.id nil]]
               :order-by [[:gl.created_at :desc]]})
  (jdbc/execute! db/pool ["SELECT * FROM gift"] db/query-opts)
  (def requester-auth0-id "auth0|abc123")
  (db/execute! db/pool
    {:select [:gl.id]
     :modifiers [:distinct]
     :from [[:gift_list :gl]]
     :join [[:invitation :i] [:= :i.gift_list_id :gl.id]
            [:invitation_acceptance :ia] [:= :ia.invitation_id :i.id]
            [:user :u] [:= :u.id :ia.accepted_by_id]]
     :where [:= :u.auth0_id requester-auth0-id]})
  )
