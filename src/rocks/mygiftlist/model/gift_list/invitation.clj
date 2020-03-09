(ns rocks.mygiftlist.model.gift-list.invitation
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
   [rocks.mygiftlist.type.gift-list.invitation-acceptance :as invitation-acceptance]
   [rocks.mygiftlist.type.gift-list.revocation :as revocation]
   [taoensso.timbre :as log])
  (:import [java.security SecureRandom]
           [java.time Instant]))

(def ^:const token-length 22)

(def ^:const token-chars
  (into []
    (mapcat (fn [[start-char end-char]]
              (mapv char (range (int start-char) (inc (int end-char))))))
    [[\a \z] [\A \Z] [\0 \9]]))

(defn- generate-token
  ([]
   (generate-token token-length))
  ([token-length]
   (let [random (SecureRandom.)]
     (apply str
       (repeatedly
         token-length
         (fn []
           (nth token-chars (.nextInt random (count token-chars)))))))))

(defn with-invitation-access-control
  "Given a query selecting invitation data with invitation aliased as i, updates
  the query so that only invitations the requester has access to are returned.

  In particular, users should only be able to access invitations for gift lists
  they have created or accepted."
  [requester-auth0-id query]
  (-> query
    (sqlh/merge-join
      [:gift_list :perm_gl] [:= :perm_gl.id :i.gift_list_id]
      [:user :perm_u] [:= :perm_u.id :perm_gl.created_by_id])
    (sqlh/merge-left-join
      [:invitation_acceptance :perm_ia] [:= :perm_ia.invitation_id :i.id]
      [:user :perm_iau] [:= :perm_iau.id :perm_ia.accepted_by_id])
    (sqlh/merge-where
      [:or
       [:= :perm_u.auth0_id requester-auth0-id]
       [:= :perm_iau.auth0_id requester-auth0-id]])))

(defresolver invitation-by-id-resolver
  [{::db/keys [pool] :keys [requester-auth0-id]} inputs]
  {::pc/input #{::invitation/id}
   ::pc/output [::invitation/token ::invitation/created-at ::invitation/expires-at
                {::invitation/created-by [::user/id]}
                {::invitation/gift-list [::gift-list/id]}]
   ::pc/transform pc/transform-batch-resolver}
  (->> {:select [:i.id :i.token :i.created_at :i.expires_at :i.created_by_id :i.gift_list_id]
        :from [[:invitation :i]]
        :where [:in :i.id (mapv ::invitation/id inputs)]}
    (with-invitation-access-control requester-auth0-id)
    (db/execute! pool)
    (mapv (fn [{::invitation/keys [created-by-id gift-list-id] :as invitation}]
            (-> invitation
              (dissoc ::invitation/created-by-id ::invitation/gift-list-id)
              (assoc-in [::invitation/created-by ::user/id] created-by-id)
              (assoc-in [::invitation/gift-list ::gift-list/id] gift-list-id))))
    (pc/batch-restore-sort {::pc/inputs inputs ::pc/key ::invitation/id})))

(defresolver invitation-by-token-resolver
  [{::db/keys [pool] :keys [requester-auth0-id]} {::invitation/keys [token]}]
  {::pc/input #{::invitation/token}
   ::pc/output [::invitation/id]}
  (db/execute-one! pool
    {:select [:i.id]
     :from [[:invitation :i]]
     :where [:= :i.token token]}))

(defresolver created-invitations-resolver
  [{::db/keys [pool] :keys [requester-auth0-id]} _]
  {::pc/output [{:created-invitations [::invitation/id]}]}
  {:created-invitations
   (db/execute! pool
     {:select [:i.id]
      :from [[:invitation :i]]
      :join [[:user :u] [:= :u.id :i.created_by_id]]
      :where [:= :u.auth0_id requester-auth0-id]})})

(def invitation-acceptance-invitation-id->invitation-id-resolver
  (pc/alias-resolver ::invitation-acceptance/invitation-id ::invitation/id))

(defmutation create-invitation
  [{::db/keys [pool] :keys [requester-auth0-id]} {::gift-list/keys [id]}]
  {::pc/params #{::gift-list/id}
   ::pc/output [::invitation/id]}
  (jdbc/with-transaction [tx pool {:isolation :serializable}]
    (when (seq (db/execute! tx
                 {:select [1]
                  :from [[:gift_list :gl]]
                  :join [[:user :u] [:= :u.id :gl.created_by_id]]
                  :where [:and
                          [:= :u.auth0_id requester-auth0-id]
                          [:= :gl.id id]]}))
      (let [token (generate-token)]
        (db/execute-one! tx
          {:insert-into :invitation
           :values [{:token token
                     :gift-list-id id
                     :created-by-id {:select [:id]
                                     :from [:user]
                                     :where [:= :auth0_id requester-auth0-id]}}]
           :returning [:id]})))))

(defmutation accept-invitation
  [{::db/keys [pool] :keys [requester-auth0-id]} {::invitation/keys [token]}]
  {::pc/params #{::invitation/token}
   ::pc/output [::invitation-acceptance/id ::invitation-acceptance/invitation-id]}
  (jdbc/with-transaction [tx pool {:isolation :serializable}]
    (let [user-didnt-create-invitation
          (empty? (db/execute! tx
                    {:select [1]
                     :from [[:invitation :i]]
                     :join [[:user :u] [:= :u.id :i.created_by_id]]
                     :where [:and
                             [:= :i.token token]
                             [:= :u.auth0_id requester-auth0-id]]}))
          invitation-hasnt-expired
          (seq (db/execute! tx
                 {:select [1]
                  :from [[:invitation :i]]
                  :where [:< (Instant/now) :i.expires_at]}))]
      (when (and user-didnt-create-invitation invitation-hasnt-expired)
        (db/execute-one! tx
          {:insert-into :invitation-acceptance
           :values [{:invitation-id {:select [:id]
                                     :from [[:invitation :i]]
                                     :where [:= :i.token token]}
                     :accepted-by-id {:select [:id]
                                      :from [[:user :u]]
                                      :where [:= :u.auth0_id requester-auth0-id]}}]
           :upsert {:on-conflict [:invitation_id :accepted_by_id]
                    ;; Using do-update-set here for no-op so that the returning
                    ;; clause returns an id when the invitation has previously
                    ;; been accepted. (Upsert semantics)
                    :do-update-set [:invitation_id :accepted_by_id]}
           :returning [:id :invitation_id]})))))

(def invitation-resolvers [invitation-by-id-resolver
                           invitation-by-token-resolver
                           created-invitations-resolver
                           invitation-acceptance-invitation-id->invitation-id-resolver
                           create-invitation
                           accept-invitation])

(comment
  (generate-token)
  )
