(ns rocks.mygiftlist.model.user
  (:require
   [datomic.client.api :as d]
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [rocks.mygiftlist.ion.query :as query]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.gift-list.invitation :as invitation]))


(defresolver all-users-resolver [{:keys [db]} input]
  {::pc/output [{:all-users [::user/id]}]}
  {:all-users (->> db
                (d/q '{:find [?id]
                       :where [[_ ::user/id ?id]]})
                (mapv (fn [[id]] {::user/id id})))})

(defresolver user-by-id-resolver [{:keys [db]} inputs]
  {::pc/input #{::user/id}
   ::pc/output [::user/auth0-id]
   ::pc/transform pc/transform-batch-resolver}
  (query/batch-query-by ::user/id
    (d/q '{:find [(pull ?u [::user/id ::user/auth0-id])]
           :in [$ [?id ...]]
           :where [[?u ::user/id ?id]]}
      db)
    inputs))

(defresolver user-by-auth0-id-resolver [{:keys [db]} inputs]
  {::pc/input #{::user/auth0-id}
   ::pc/output [::user/id]
   ::pc/transform pc/transform-batch-resolver}
  (query/batch-query-by ::user/auth0-id
    (d/q '{:find [(pull ?u [::user/auth0-id ::user/id])]
           :in [$ [?auth0-id ...]]
           :where [[?u ::user/auth0-id ?auth0-id]]}
      db)
    inputs))

(defresolver user-by-email-resolver [{:keys [db requester-auth0-id]} inputs]
  {::pc/input #{::user/email}
   ::pc/output [::user/id ::user/auth0-id]
   ::pc/transform pc/transform-batch-resolver}
  (query/batch-query-by ::user/email
    (d/q '{:find [(pull ?u [::user/email ::user/id ::user/auth0-id])]
           :in [$ ?requester-auth0-id [?email ...]]
           :where [[?u ::user/email ?email]
                   (or-join [?u ?requester-auth0-id]
                     [?u ::user/auth0-id ?requester-auth0-id]
                     (and
                       [?gift-list ::gift-list/created-by ?u]
                       [?invitation ::invitation/gift-list ?gift-list]
                       [?invitation ::invitation/accepted-by ?accepter]
                       [?accepter ::user/auth0-id ?requester-auth0-id])
                     (and
                       [?list-owner ::user/auth0-id ?requester-auth0-id]
                       [?gift-list ::gift-list/created-by ?list-owner]
                       [?invitation ::invitation/gift-list ?gift-list]
                       [?invitation ::invitation/accepted-by ?u]))]}
      db requester-auth0-id)
    inputs))

(defresolver user-email-resolver [{:keys [db requester-auth0-id]} inputs]
  {::pc/input #{::user/id}
   ::pc/output [::user/email]
   ::pc/transform pc/transform-batch-resolver}
  (query/batch-query-by ::user/id
    (d/q '{:find [(pull ?u [::user/id ::user/email])]
           :in [$ ?requester-auth0-id [?id ...]]
           :where [(or-join [?u ?id ?requester-auth0-id]
                     (and
                       [?u ::user/auth0-id ?requester-auth0-id]
                       [?u ::user/id ?id])
                     (and
                       [?gift-list ::gift-list/created-by ?u]
                       [?invitation ::invitation/gift-list ?gift-list]
                       [?invitation ::invitation/accepted-by ?accepter]
                       [?accepter ::user/auth0-id ?requester-auth0-id])
                     (and
                       [?list-owner ::user/auth0-id ?requester-auth0-id]
                       [?gift-list ::gift-list/created-by ?list-owner]
                       [?invitation ::invitation/gift-list ?gift-list]
                       [?invitation ::invitation/accepted-by ?u]))]}
      db requester-auth0-id)
    inputs))

(defresolver user-personal-data-resolver [{:keys [db requester-auth0-id]} inputs]
  {::pc/input #{::user/id}
   ::pc/output [::user/given-name ::user/family-name]
   ::pc/transform pc/transform-batch-resolver}
  (query/batch-query-by ::user/id
    (d/q '{:find [(pull ?u [::user/id ::user/given-name ::user/family-name])]
           :in [$ ?requester-auth0-id [?id ...]]
           :where [[?u ::user/id ?id]
                   (or-join [?u ?requester-auth0-id]
                     [?u ::user/auth0-id ?requester-auth0-id]
                     (and
                       [?u ::user/display-name true]
                       [?gift-list ::gift-list/created-by ?u]
                       [?invitation ::invitation/gift-list ?gift-list]
                       [?invitation ::invitation/accepted-by ?accepter]
                       [?accepter ::user/auth0-id ?requester-auth0-id])
                     (and
                       [?u ::user/display-name true]
                       [?list-owner ::user/auth0-id ?requester-auth0-id]
                       [?gift-list ::gift-list/created-by ?list-owner]
                       [?invitation ::invitation/gift-list ?gift-list]
                       [?invitation ::invitation/accepted-by ?u]))]}
      db requester-auth0-id)
    inputs))

(def user-resolvers
  [all-users-resolver
   user-by-id-resolver
   user-by-auth0-id-resolver
   user-by-email-resolver
   user-email-resolver
   user-personal-data-resolver
   ])

(comment
  )
