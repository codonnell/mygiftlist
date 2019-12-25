(ns rocks.mygiftlist.ion.schema
  (:require [rocks.mygiftlist.type.gift :as gift]
            [rocks.mygiftlist.type.gift-list :as gift-list]
            [rocks.mygiftlist.type.gift-list.invitation :as invitation]
            [rocks.mygiftlist.type.gift-list.revocation :as revocation]
            [rocks.mygiftlist.type.user :as user]))

(def user-schema
  [{:db/ident ::user/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A user id"}
   {:db/ident ::user/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The instant when a user was created"}
   {:db/ident ::user/auth0-id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A user's auth0 id"}
   {:db/ident ::user/email
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A user's email"}
   {:db/ident ::user/given-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A user's given name"}
   {:db/ident ::user/family-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A user's family name"}
   {:db/ident ::user/display-name
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Whether the user chooses to have their name displayed"}])

(def gift-schema
  [{:db/ident ::gift/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A gift's id"}
   {:db/ident ::gift/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A gift's name"}
   {:db/ident ::gift/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A gift's description"}
   {:db/ident ::gift/url
    :db/valueType :db.type/uri
    :db/cardinality :db.cardinality/one
    :db/doc "A URL for the gift"}
   {:db/ident ::gift/claimed-by
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The user who claimed this gift"}
   {:db/ident ::gift/claimed-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The instant this gift was claimed"}
   {:db/ident ::gift/requested-by
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The user who requested this gift"}
   {:db/ident ::gift/requested-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}])

(def gift-list-schema
  [{:db/ident ::gift-list/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A gift list's id"}
   {:db/ident ::gift-list/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A gift list's name"}
   {:db/ident ::gift-list/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The instant a gift list was created"}
   {:db/ident ::gift-list/created-by
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The user who created the gift list"}
   {:db/ident ::gift-list/gifts
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "The gifts on this gift list"}])

(def invitation-schema
  [{:db/ident ::invitation/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "An invitation's id"}
   {:db/ident ::invitation/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The instant an invitation was created"}
   {:db/ident ::invitation/created-by
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The user who created the invitation"}
   {:db/ident ::invitation/token
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "The invitation's unique token"}
   {:db/ident ::invitation/expires-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The time when an invitation expires"}
   {:db/ident ::invitation/accepted-by
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "The users who accepted this invitation"}
   {:db/ident ::invitation/gift-list
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The gift list this invitation is for"}])

(def revocation-schema
  [{:db/ident ::revocation/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A revocation's id"}
   {:db/ident ::revocation/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The instant a revocation was created"}
   {:db/ident ::revocation/created-by
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The user who created the revocation"}
   {:db/ident ::revocation/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The users whose access has been revoked"}
   {:db/ident ::revocation/gift-list
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The gift list this revocation is for"}])

(def schema
  (into [] cat
    [user-schema
     gift-schema
     gift-list-schema
     invitation-schema
     revocation-schema]))
