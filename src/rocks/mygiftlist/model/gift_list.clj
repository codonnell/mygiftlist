(ns rocks.mygiftlist.model.gift-list
  (:require
   [datomic.client.api :as d]
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [rocks.mygiftlist.ion.query :as query]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.gift-list.invitation :as invitation]
   [rocks.mygiftlist.type.gift-list.revocation :as revocation]))

(defresolver gift-list-by-id-resolver [{:keys [db requester-auth0-id]} inputs]
  {::pc/input #{::gift-list/id}
   ::pc/output [::gift-list/name ::gift-list/created-at ::gift-list/created-by
                {::gift-list/gifts [::gift/id]}]
   ::pc/transform pc/transform-batch-resolver}
  (query/batch-query-by ::gift-list/id
    (d/q '{:find [(pull ?gift-list [::gift-list/id ::gift-list/name
                               ::gift-list/created-at ::gift-list/created-by
                               {::gift-list/gifts [::gift/id]}])]
           :in [$ ?requester-auth0-id [?id ...]]
           :where [[?requester ::user/auth0-id ?requester-auth0-id]
                   [?gift-list ::gift-list/id ?id]
                   (or-join [?gift-list ?requester]
                     [?gift-list ::gift-list/created-by ?requester]
                     (and
                       [?invitation ::invitation/gift-list ?gift-list]
                       [?invitation ::invitation/accepted-by ?requester]
                       (not-join [?gift-list]
                         [?revocation ::revocation/gift-list ?gift-list]
                         [?revocation ::revocation/user ?requester])))]}
      db requester-auth0-id)
    inputs))

(def gift-list-resolvers [gift-list-by-id-resolver])
