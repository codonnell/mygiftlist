(ns rocks.mygiftlist.model.gift
  (:require
   [datomic.client.api :as d]
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [rocks.mygiftlist.ion.query :as query]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.gift-list.invitation :as invitation]
   [rocks.mygiftlist.type.gift-list.revocation :as revocation]))

(defresolver gift-by-id-resolver [{:keys [db requester-auth0-id]} inputs]
  {::pc/input #{::gift/id}
   ::pc/output [::gift/name ::gift/description ::gift/url
                {::gift/claimed-by [::user/id]} ::gift/claimed-at
                {::gift/requested-by [::user/id]} ::gift/requested-at]
   ::pc/transform pc/transform-batch-resolver}
  (query/batch-query-by ::gift/id
    (d/q '{:find [(pull ?gift [::gift/id ::gift/name ::gift/description ::gift/url
                               {::gift/claimed-by [::user/id]} ::gift/claimed-at
                               {::gift/requested-by [::user/id]} ::gift/requested-at])]
           :in [$ ?requester-auth0-id [?id ...]]
           :where [[?requester ::user/auth0-id ?requester-auth0-id]
                   [?gift ::gift/id ?id]
                   [?gift-list ::gift-list/gifts ?gift]
                   (or-join [?gift-list ?requester]
                     [?requester ::gift/requested-by ?requester]
                     (and
                       [?invitation ::invitation/gift-list ?gift-list]
                       [?invitation ::invitation/accepted-by ?requester]
                       (not-join [?gift-list]
                         [?revocation ::revocation/gift-list ?gift-list]
                         [?revocation ::revocation/user ?requester])))]}
      db requester-auth0-id)
    inputs))

(def gift-resolvers [gift-by-id-resolver])
