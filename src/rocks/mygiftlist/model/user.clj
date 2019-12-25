(ns rocks.mygiftlist.model.user
  (:require
   [datomic.client.api :as d]
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [rocks.mygiftlist.model.gift-list :as gift-list]
   [rocks.mygiftlist.model.gift-list.invitation :as invitation]))


(defresolver all-users-resolver [{:keys [db]} input]
  {::pc/output [{:all-users [::id]}]}
  {:all-users (->> db
                (d/q '{:find [?id]
                       :where [[_ ::id ?id]]})
                (mapv (fn [[id]] {::id id})))})

(defresolver user-by-id-resolver [{:keys [db]} {::keys [id]}]
  {::pc/input #{::id}
   ::pc/output [::auth0-id]}
  (ffirst (d/q '{:find [(pull ?u [::auth0-id])]
                 :in [$ ?id]
                 :where [[?u ::id ?id]]}
            db id)))

(defresolver user-by-auth0-id-resolver [{:keys [db]} {::keys [auth0-id]}]
  {::pc/input #{::auth0-id}
   ::pc/output [::id]}
  (ffirst (d/q '{:find [(pull ?u [::id])]
                 :in [$ ?auth0-id]
                 :where [[?u ::auth0-id ?auth0-id]]}
            db auth0-id)))

(defresolver user-by-email-resolver [{:keys [db requester-auth0-id]} {::keys [email]}]
  {::pc/input #{::email}
   ::pc/output [::id ::auth0-id]}
  (let [q-where (fn [where-clause]
                  (ffirst (d/q (assoc '{:find [(pull ?u [::id ::auth0-id])]
                                        :in [$ ?requester-auth0-id ?email]}
                                 :where where-clause)
                            db requester-auth0-id email)))]
    (first (sequence (comp (take 1) (keep q-where))
             '[[[?u ::email ?email]
                [?u ::auth0-id ?requester-auth0-id]]

               [[?u ::email ?email]
                [?gift-list ::gift-list/created-by ?u]
                [?invitation ::invitation/gift-list ?gift-list]
                [?invitation ::invitation/accepted-by ?accepter]
                [?accepter ::auth0-id ?requester-auth0-id]]

               [[?u ::email ?email]
                [?list-owner ::auth0-id ?requester-auth0-id]
                [?gift-list ::gift-list/created-by ?list-owner]
                [?invitation ::invitation/gift-list ?gift-list]
                [?invitation ::invitation/accepted-by ?u]]]))))

(defresolver user-email-resolver [{:keys [db requester-auth0-id]} {::keys [id]}]
  {::pc/input #{::id}
   ::pc/output [::email]}
  (let [q-where (fn [where-clause]
                  (ffirst (d/q (assoc '{:find [(pull ?u [::email])]
                                        :in [$ ?requester-auth0-id ?id]}
                                 :where where-clause)
                            db requester-auth0-id id)))]
    (first (sequence (comp (take 1) (keep q-where))
             '[[[?u ::auth0-id ?requester-auth0-id]]

               [[?gift-list ::gift-list/created-by ?u]
                [?invitation ::invitation/gift-list ?gift-list]
                [?invitation ::invitation/accepted-by ?accepter]
                [?accepter ::auth0-id ?requester-auth0-id]]

               [[?list-owner ::auth0-id ?requester-auth0-id]
                [?gift-list ::gift-list/created-by ?list-owner]
                [?invitation ::invitation/gift-list ?gift-list]
                [?invitation ::invitation/accepted-by ?u]]]))))

(defresolver user-personal-data-resolver [{:keys [db requester-auth0-id]} {::keys [id]}]
  {::pc/input #{::id}
   ::pc/output [::given-name ::family-name]}
  (let [q-where (fn [where-clause]
                  (ffirst (d/q (assoc '{:find [(pull ?u [::given-name ::family-name])]
                                        :in [$ ?requester-auth0-id ?id]}
                                 :where where-clause)
                            db requester-auth0-id id)))]
    (first (sequence (comp (take 1) (keep q-where))
             '[[[?u ::id ?id]
                [?u ::auth0-id ?requester-auth0-id]]

               [[?u ::id ?id]
                [?u ::display-name true]
                [?gift-list ::gift-list/created-by ?u]
                [?invitation ::invitation/gift-list ?gift-list]
                [?invitation ::invitation/accepted-by ?accepter]
                [?accepter ::auth0-id ?requester-auth0-id]]

               [[?u ::id ?id]
                [?u ::display-name true]
                [?list-owner ::auth0-id ?requester-auth0-id]
                [?gift-list ::gift-list/created-by ?list-owner]
                [?invitation ::invitation/gift-list ?gift-list]
                [?invitation ::invitation/accepted-by ?u]]]))))

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
