(ns rocks.mygiftlist.model.user
  (:require
   [datomic.client.api :as d]
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [rocks.mygiftlist.ion.query :as query]
   [rocks.mygiftlist.model.gift-list :as gift-list]
   [rocks.mygiftlist.model.gift-list.invitation :as invitation]))


(defresolver all-users-resolver [{:keys [db]} input]
  {::pc/output [{:all-users [::id]}]}
  {:all-users (->> db
                (d/q '{:find [?id]
                       :where [[_ ::id ?id]]})
                (mapv (fn [[id]] {::id id})))})

(defresolver user-by-id-resolver [{:keys [db]} inputs]
  {::pc/input #{::id}
   ::pc/output [::auth0-id]
   ::pc/transform pc/transform-batch-resolver}
  (query/batch-query-by ::id
    (d/q '{:find [(pull ?u [::id ::auth0-id])]
           :in [$ [?id ...]]
           :where [[?u ::id ?id]]}
      db)
    inputs))

(defresolver user-by-auth0-id-resolver [{:keys [db]} inputs]
  {::pc/input #{::auth0-id}
   ::pc/output [::id]
   ::pc/transform pc/transform-batch-resolver}
  (query/batch-query-by ::auth0-id
    (d/q '{:find [(pull ?u [::auth0-id ::id])]
           :in [$ [?auth0-id ...]]
           :where [[?u ::auth0-id ?auth0-id]]}
      db)
    inputs))

(defresolver user-by-email-resolver [{:keys [db requester-auth0-id]} inputs]
  {::pc/input #{::email}
   ::pc/output [::id ::auth0-id]
   ::pc/transform pc/transform-batch-resolver}
  (query/batch-query-by ::email
    (d/q '{:find [(pull ?u [::email ::id ::auth0-id])]
           :in [$ ?requester-auth0-id [?email ...]]
           :where [[?u ::email ?email]
                   (or-join [?u ?requester-auth0-id]
                     [?u ::auth0-id ?requester-auth0-id]
                     (and
                       [?gift-list ::gift-list/created-by ?u]
                       [?invitation ::invitation/gift-list ?gift-list]
                       [?invitation ::invitation/accepted-by ?accepter]
                       [?accepter ::auth0-id ?requester-auth0-id])
                     (and
                       [?list-owner ::auth0-id ?requester-auth0-id]
                       [?gift-list ::gift-list/created-by ?list-owner]
                       [?invitation ::invitation/gift-list ?gift-list]
                       [?invitation ::invitation/accepted-by ?u]))]}
      db requester-auth0-id)
    inputs))

(defresolver user-email-resolver [{:keys [db requester-auth0-id]} inputs]
  {::pc/input #{::id}
   ::pc/output [::email]
   ::pc/transform pc/transform-batch-resolver}
  (query/batch-query-by ::id
    (d/q '{:find [(pull ?u [::id ::email])]
           :in [$ ?requester-auth0-id [?id ...]]
           :where [(or-join [?u ?id ?requester-auth0-id]
                     (and
                       [?u ::auth0-id ?requester-auth0-id]
                       [?u ::id ?id])
                     (and
                       [?gift-list ::gift-list/created-by ?u]
                       [?invitation ::invitation/gift-list ?gift-list]
                       [?invitation ::invitation/accepted-by ?accepter]
                       [?accepter ::auth0-id ?requester-auth0-id])
                     (and
                       [?list-owner ::auth0-id ?requester-auth0-id]
                       [?gift-list ::gift-list/created-by ?list-owner]
                       [?invitation ::invitation/gift-list ?gift-list]
                       [?invitation ::invitation/accepted-by ?u]))]}
      db requester-auth0-id)
    inputs))

(defresolver user-personal-data-resolver [{:keys [db requester-auth0-id]} inputs]
  {::pc/input #{::id}
   ::pc/output [::given-name ::family-name]
   ::pc/transform pc/transform-batch-resolver}
  (query/batch-query-by ::id
    (d/q '{:find [(pull ?u [::id ::given-name ::family-name])]
           :in [$ ?requester-auth0-id [?id ...]]
           :where [[?u ::id ?id]
                   (or-join [?u ?requester-auth0-id]
                     [?u ::auth0-id ?requester-auth0-id]
                     (and
                       [?u ::display-name true]
                       [?gift-list ::gift-list/created-by ?u]
                       [?invitation ::invitation/gift-list ?gift-list]
                       [?invitation ::invitation/accepted-by ?accepter]
                       [?accepter ::auth0-id ?requester-auth0-id])
                     (and
                       [?u ::display-name true]
                       [?list-owner ::auth0-id ?requester-auth0-id]
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
