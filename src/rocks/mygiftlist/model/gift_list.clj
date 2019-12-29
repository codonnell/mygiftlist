(ns rocks.mygiftlist.model.gift-list
  (:require
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [next.jdbc :as jdbc]
   [honeysql.core :as sql]
   [rocks.mygiftlist.server-components.db :as db]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.gift-list.invitation :as invitation]
   [rocks.mygiftlist.type.gift-list.revocation :as revocation]
   [taoensso.timbre :as log]))

;; TODO: Add permissions
;; TODO: Make sure pool env putter-inner works
(defresolver gift-list-by-id-resolver [{::db/keys [pool] :keys [requester-auth0-id]} inputs]
  {::pc/input #{::gift-list/id}
   ::pc/output [::gift-list/name ::gift-list/created-at
                {::gift-list/created-by [::user/id]}
                {::gift-list/gifts [::gift/id]}]
   ::pc/transform pc/transform-batch-resolver}
  (let [ids (mapv ::gift-list/id inputs)
        raw-results (db/execute! db/pool
                      {:select [:gl.id :gl.name :gl.created_at :u.id
                                [(sql/call :array_agg :g.id) :gift_ids]]
                       :from [[:gift_list :gl]]
                       :left-join [[:gift :g] [:= :g.gift_list_id :gl.id]
                                   [:user :u] [:= :u.id :gl.created_by_id]]
                       :where [:in :gl.id ids]
                       :group-by [:gl.id :gl.name :gl.created_at :gl.created_by_id :u.id]})
        results (mapv (fn [{:keys [gift-ids] :as gift-list}]
                        (-> gift-list
                          (assoc ::gift-list/gifts
                            (mapv #(hash-map ::gift/id %) gift-ids))
                          (assoc-in [::gift-list/created-by ::user/id]
                            (::user/id gift-list))
                          (dissoc :gift-ids ::user/id)))
                  raw-results)]
    (pc/batch-restore-sort {::pc/inputs inputs ::pc/key ::gift-list/id} results)))

(defresolver created-gift-lists-resolver [{::db/keys [pool] :keys [requester-auth0-id]} _]
  {::pc/output [{:created-gift-lists [::gift-list/id]}]}
  {:created-gift-lists
   (db/execute! db/pool
     {:select [:gl.id]
      :modifiers [:distinct]
      :from [[:gift_list :gl]]
      :join [[:user :u] [:= :u.id :gl.created_by_id]]
      :where [:= :u.auth0_id requester-auth0-id]})})

(defresolver invited-gift-lists-resolver [{::db/keys [pool] :keys [requester-auth0-id]} _]
  {::pc/output [{:invited-gift-lists [::gift-list/id]}]}
  {:invited-gift-lists
   (db/execute! db/pool
     {:select [:gl.id]
      :modifiers [:distinct]
      :from [[:gift_list :gl]]
      :join [[:invitation :i] [:= :i.gift_list_id :gl.id]
             [:invitation_acceptance :ia] [:= :ia.invitation_id :i.id]
             [:user :u] [:= :u.id :ia.accepted_by_id]]
      :where [:= :u.auth0_id requester-auth0-id]})})

;; (defresolver invited-gift-lists-resolver [{:keys [db requester-auth0-id]} _]
;;   {::pc/output [{:invited-gift-lists [::gift-list/id]}]}
;;   {:invited-gift-lists (mapv first (d/q '{:find [(pull ?gift-list [::gift-list/id])]
;;                                           :in [$ ?requester-auth0-id]
;;                                           :where [[?user ::user/auth0-id ?requester-auth0-id]
;;                                                   [?invitation ::invitation/accepted-by ?user]
;;                                                   [?invitation ::invitation/gift-list ?gift-list]
;;                                                   (not-join [?gift-list ?user]
;;                                                     [?revocation ::revocation/user ?user]
;;                                                     [?revocation ::revocation/gift-list ?gift-list])]}
;;                                      db requester-auth0-id))})

(def gift-list-resolvers
  [gift-list-by-id-resolver
   created-gift-lists-resolver
   invited-gift-lists-resolver
   ])

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
