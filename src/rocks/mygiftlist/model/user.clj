(ns rocks.mygiftlist.model.user
  (:require
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [rocks.mygiftlist.server-components.db :as db]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.gift-list.invitation :as invitation]
   [rocks.mygiftlist.type.gift-list.revocation :as revocation]
   [honeysql.core :as sql]
   [honeysql.helpers :as sqlh]))

(defn with-user-access-control
  "Given a query selecting user aliased as u, updates the query so that only
  user data the requester has access to are returned.

  In particular, a requester should only be able to access user data when the
  requester has accepted an invitation to a gift list belonging to the user, the
  user has accepted an invitation belonging to the requester, or the user is the
  requester."
  [requester-auth0-id query]
  (-> query
    (sqlh/merge-left-join
      ;; Joins to find when requester invited the user to a gift list
      [:invitation_acceptance :perm_ia] [:= :perm_ia.accepted_by_id :u.id]
      [:invitation :perm_i] [:= :perm_i.id :perm_ia.invitation_id]
      [:gift_list :perm_gl] [:= :perm_gl.id :perm_i.gift_list_id]
      [:user :perm_u] [:= :perm_u.id :perm_gl.created_by_id]

      ;; Joins to find when user invited the requester to a gift list
      [:gift_list :perm_gl_rev] [:= :perm_gl_rev.created_by_id :u.id]
      [:invitation :perm_i_rev] [:= :perm_i_rev.gift_list_id :perm_gl_rev.id]
      [:invitation_acceptance :perm_ia_rev] [:= :perm_ia_rev.invitation_id :perm_i_rev.id]
      [:user :perm_u_rev] [:= :perm_u_rev.id :perm_ia_rev.accepted_by_id])
    (sqlh/merge-where
      [:or
       [:= requester-auth0-id :perm_u.auth0_id]
       [:= requester-auth0-id :perm_u_rev.auth0_id]
       [:= requester-auth0-id :u.auth0_id]])))

(defresolver user-by-id-resolver [{::db/keys [pool] :keys [requester-auth0-id]} inputs]
  {::pc/input #{::user/id}
   ::pc/output [::user/auth0-id ::user/email ::user/created-at]
   ::pc/transform pc/transform-batch-resolver}
  (->> {:select [:u.id :u.auth0_id :u.email :u.created_at]
        :modifiers [:distinct]
        :from [[:user :u]]
        :where [:in :u.id (mapv ::user/id inputs)]}
    (with-user-access-control requester-auth0-id)
    (db/execute! pool)
    (pc/batch-restore-sort
      {::pc/key ::user/id
       ::pc/inputs inputs})))

(defresolver user-by-auth0-id-resolver [{::db/keys [pool] :keys [requester-auth0-id]} inputs]
  {::pc/input #{::user/auth0-id}
   ::pc/output [::user/id ::user/email ::user/created-at]
   ::pc/transform pc/transform-batch-resolver}
  (->> {:select [:u.id :u.auth0_id :u.email :u.created_at]
        :modifiers [:distinct]
        :from [[:user :u]]
        :where [:in :u.auth0_id (mapv ::user/auth0-id inputs)]}
    (with-user-access-control requester-auth0-id)
    (db/execute! pool)
    (pc/batch-restore-sort
      {::pc/key ::user/auth0-id
       ::pc/inputs inputs})))

(defresolver user-by-email-resolver [{::db/keys [pool] :keys [requester-auth0-id]} inputs]
  {::pc/input #{::user/email}
   ::pc/output [::user/id ::user/auth0-id ::user/created-at]
   ::pc/transform pc/transform-batch-resolver}
  (->>
    {:select [:u.email :u.id :u.auth0_id :u.created_at]
     :modifiers [:distinct]
     :from [[:user :u]]
     :where [:in :u.email (mapv ::user/email inputs)]}
    (with-user-access-control requester-auth0-id)
    (db/execute! pool)
    (pc/batch-restore-sort
      {::pc/key ::user/email
       ::pc/inputs inputs})))

(defresolver user-name-resolver [{::db/keys [pool] :keys [requester-auth0-id]} inputs]
  {::pc/input #{::user/id}
   ::pc/output [::user/given-name ::user/family-name]
   ::pc/transform pc/transform-batch-resolver}
  (->> {:select [:u.id :u.given_name :u.family_name]
        :modifiers [:distinct]
        :from [[:user :u]]
        :where [:and
                [:in :u.id (mapv ::user/id inputs)]
                [:= true :u.allow_name_access]]}
    (with-user-access-control requester-auth0-id)
    (db/execute! pool)
    (pc/batch-restore-sort
      {::pc/key ::user/id
       ::pc/inputs inputs})))

(defresolver user-join-resolver [{::db/keys [pool] :keys [requester-auth0-id]} {::user/keys [id]}]
  {::pc/input #{::user/id}
   ::pc/output [{::user/requested-gifts [::gift/id]}
                {::user/claimed-gifts [::gift/id]}
                {::user/created-gift-lists [::gift-list/id]}
                {::user/accepted-invitations [::invitation/id]}
                {::user/created-invitations [::invitation/id]}
                {::user/revocations [::revocation/id]}
                {::user/created-revocations [::revocation/id]}]}
  (let [{:keys [requested-gift-ids
                claimed-gift-ids
                created-gift-list-ids
                accepted-invitation-ids
                created-invitation-ids
                revocation-ids
                created-revocation-ids]}
        (->> {:select [[(sql/call :array_agg_distinct :requested_gift.id) :requested_gift_ids]
                       [(sql/call :array_agg_distinct :claimed_gift.id) :claimed_gift_ids]
                       [(sql/call :array_agg_distinct :created_gift_list.id) :created_gift_list_ids]
                       [(sql/call :array_agg_distinct :accepted_invitation.id) :accepted_invitation_ids]
                       [(sql/call :array_agg_distinct :created_invitation.id) :created_invitation_ids]
                       [(sql/call :array_agg_distinct :r.id) :revocation_ids]
                       [(sql/call :array_agg_distinct :created_revocation.id) :created_revocation_ids]]
              :from [[:user :u]]
              :left-join [[:gift :requested_gift] [:= :requested_gift.requested_by_id :u.id]
                          [:gift :claimed_gift] [:= :claimed_gift.claimed_by_id :u.id]
                          [:gift_list :created_gift_list] [:= :created_gift_list.created_by_id :u.id]
                          [:invitation_acceptance :ia] [:= :ia.accepted_by_id :u.id]
                          [:invitation :accepted_invitation] [:= :accepted_invitation.id :ia.invitation_id]
                          [:invitation :created_invitation] [:= :created_invitation.created_by_id :u.id]
                          [:revocation :r] [:= :r.revoked_user_id :u.id]
                          [:revocation :created_revocation] [:= :created_revocation.created_by_id :u.id]]
              :where [:and
                      [:= :u.id id]
                      [:= :u.auth0_id requester-auth0-id]]
              :group-by [:u.id]}
          (db/execute-one! pool)
          (into {} (map (fn [[k v]] [k (filterv some? v)]))))]
    {::user/requested-gifts (mapv #(hash-map ::gift/id %) requested-gift-ids)
     ::user/claimed-gifts (mapv #(hash-map ::gift/id %) claimed-gift-ids)
     ::user/created-gift-lists (mapv #(hash-map ::gift-list/id %) created-gift-list-ids)
     ::user/accepted-invitations (mapv #(hash-map ::invitation/id %) accepted-invitation-ids)
     ::user/created-invitations (mapv #(hash-map ::invitation/id %) created-invitation-ids)
     ::user/revocations (mapv #(hash-map ::revocation/id %) revocation-ids)
     ::user/created-revocations (mapv #(hash-map ::revocation/id %) created-revocation-ids)}))

(defmutation upsert-user-on-auth0-id [{::db/keys [pool] :keys [requester-auth0-id] :as env} {::user/keys [auth0-id email]}]
  {::pc/params #{::user/auth0-id ::user/email}
   ::pc/output [::user/id]}
  (when (= requester-auth0-id auth0-id)
    (db/execute-one! pool
      {:insert-into :user
       :values [{:auth0_id auth0-id
                 :email email}]
       :upsert {:on-conflict [:auth0_id]
                :do-update-set [:email]}
       :returning [:id]})))

(def user-resolvers
  [user-by-id-resolver
   user-by-auth0-id-resolver
   user-by-email-resolver
   user-name-resolver
   user-join-resolver

   upsert-user-on-auth0-id])

(comment
  (sql/format
    {:select [[(sql/call :array_agg_distinct :requested_gift.id) :requested_gift_ids]
              [(sql/call :array_agg_distinct :claimed_gift.id) :claimed_gift_ids]
              [(sql/call :array_agg_distinct :created_gift_list.id) :created_gift_list_ids]
              [(sql/call :array_agg_distinct :accepted_invitation.id) :accepted_invitation_ids]
              [(sql/call :array_agg_distinct :created_invitation.id) :created_invitation_ids]
              [(sql/call :array_agg_distinct :r.id) :revocation_ids]
              [(sql/call :array_agg_distinct :created_revocation.id) :created_revocation_ids]]
     :from [[:user :u]]
     :left-join [
                 [:gift :requested_gift] [:= :requested_gift.requested_by_id :u.id]
                 [:gift :claimed_gift] [:= :claimed_gift.claimed_by_id :u.id]
                 [:gift_list :created_gift_list] [:= :created_gift_list.created_by_id :u.id]
                 [:invitation_acceptance :ia] [:= :ia.accepted_by_id :u.id]
                 [:invitation :accepted_invitation] [:= :accepted_invitation.id :ia.invitation_id]
                 [:invitation :created_invitation] [:= :created_invitation.created_by_id :u.id]
                 [:revocation :r] [:= :r.revoked_user_id :u.id]
                 [:revocation :created_revocation] [:= :created_revocation.created_by_id :u.id]
                 ]
     :where [:and
             [:= :u.id "abc"]
             [:= :u.auth0_id "abc123"]]
     :group-by [:u.id]}
    :quoting :ansi)
  )
