(ns rocks.mygiftlist.ion
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [datomic.client.api :as d]
   [rocks.mygiftlist.config :as config]
   [rocks.mygiftlist.ion.schema :as schema]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list])
  (:import [java.util Date UUID]))

(def get-client
  "Return a shared client. Set datomic/ion/starter/config.edn resource
before calling this function."
  (memoize #(if-let [r (io/resource "datomic/ion/mygiftlistrocks/config.edn")]
              (d/client (edn/read-string (slurp r)))
              (throw (RuntimeException. "You need to add a resource datomic/ion/mygiftlistrocks/config.edn with your connection config")))))

(defn get-connection []
  (d/connect (get-client) {:db-name (:db-name (config/get-config config/environment))}))

(defn get-db []
  (d/db (get-connection)))

(defn get-user-by-auth0-id [query auth0-id]
  (ffirst (d/q '{:find [(pull ?u pattern)]
                 :in [$ pattern ?auth0-id]
                 :where [[?u ::user/auth0-id ?auth0-id]]}
            (get-db) query auth0-id)))

(comment
  (def client (get-client))
  (d/delete-database (get-client) {:db-name "my-db"})
  (def db-name "my-db")
  (d/create-database client {:db-name db-name})
  (def conn (d/connect client {:db-name db-name}))
  (d/transact conn {:tx-data schema/schema})
  (d/transact (get-connection) {:tx-data [#::user {:id (UUID/randomUUID)
                                                   :auth0-id "auth0|5dfeec6f9567eb0dc0302207"
                                                   :email "chris@example.com"
                                                   :created-at (Date.)
                                                   :given-name "Chris"
                                                   :family-name "Example"}]})
  (d/transact (get-connection) {:tx-data [#::user {:id (UUID/randomUUID)
                                                   :auth0-id "auth0|fake"
                                                   :email "alice@example.com"
                                                   :created-at (Date.)
                                                   :given-name "Alice"
                                                   :family-name "Example"}]})
  (d/q '{:find [?id ?name]
         :where [[?gift ::gift/id ?id]
                 [?gift ::gift/name ?name]]}
    (get-db))
  (def bob [::user/auth0-id "auth0|5dc81bfc1658c30e5fe9b877"])
  (def chris [::user/auth0-id "auth0|5dfeec6f9567eb0dc0302207"])
  (d/transact (get-connection)
    {:tx-data [#::gift-list {:id #uuid "0ebaf3ee-7d0d-4573-880a-ad2cb8582ec7"
                             :name "Bob's Birthday"
                             :created-at (Date.)
                             :created-by chris
                             :gifts [#::gift {:id #uuid "6b16987d-5208-4142-a3cd-ed1c3a7c7882"
                                              :name "Socks"
                                              :description "Wool and warm"
                                              :url (java.net.URI. "https://example.com/socks")
                                              :requested-by chris
                                              :requested-at (Date.)}
                                     #::gift {:id #uuid "33f97c96-6e33-4b03-8c0f-5f8e12da3200"
                                              :name "Pants"
                                              :description "Wool and warm"
                                              :url (java.net.URI. "https://example.com/pants")
                                              :requested-by chris
                                              :requested-at (Date.)}]}]})
  (d/q '{:find [?gift-list-id]
         :where [[_ ::gift-list/id ?gift-list-id]]}
    (get-db))
  (d/q '{:find [(pull ?u [::user/id ::user/auth0-id ::user/email])]
         :where [[?u ::user/id]]}
    (get-db))
  (get-user-by-auth0-id [::user/id ::user/auth0-id ::user/email ::user/given-name ::user/family-name]
    "auth0|5dc81bfc1658c30e5fe9b877")
  (d/q '{:find [(pull ?u [::user/id])]
         :in [$ ?requester-id ?id]
         :where [(or-join [?u]
                   (and
                     [?u ::user/id ?id]
                     [?u ::user/id ?requester-id])
                   (and
                     [?u ::user/id ?id]
                     [?requester ::user/id ?requester-id]
                     [?grant :view-grant/grantor ?u]
                     [?grant :view-grant/grantee ?requester]))]}
    (get-db) (UUID/randomUUID) (UUID/randomUUID))
  )
