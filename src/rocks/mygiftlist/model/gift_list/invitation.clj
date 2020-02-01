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
   [rocks.mygiftlist.type.gift-list.revocation :as revocation]
   [taoensso.timbre :as log])
  (:import [java.security SecureRandom]))

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
  they have created."
  [requester-auth0-id query]
  (-> query
    (sqlh/merge-join
      [:gift_list :perm_gl] [:= :perm_gl.id :i.gift_list_id]
      [:user :perm_u] [:= :perm_u.id :perm_gl.created_by_id])
    (sqlh/merge-where
      [:= :perm_u.auth0_id requester-auth0-id])))

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

(defmutation create-invitation
  [{::db/keys [pool] :keys [requester-auth0-id]} {::gift-list/keys [id]}]
  {::pc/params #{::gift-list/id}
   ::pc/output [::invitation/id]}
  (when (seq (db/execute! pool
               {:select [1]
                :from [[:gift_list :gl]]
                :join [[:user :u] [:= :u.id :gl.created_by_id]]
                :where [:and
                        [:= :u.auth0_id requester-auth0-id]
                        [:= :gl.id id]]}))
    (let [token (generate-token)]
      (db/execute-one! pool
        {:insert-into :invitation
         :values [{:token token
                   :gift-list-id id
                   :created-by-id {:select [:id]
                                   :from [:user]
                                   :where [:= :auth0_id requester-auth0-id]}}]
         :returning [:id]}))))

(def invitation-resolvers [invitation-by-id-resolver create-invitation])

(comment
  (generate-token)
  )
