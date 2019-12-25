(ns rocks.mygiftlist.ion
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [datomic.client.api :as d]
   [rocks.mygiftlist.config :as config]
   [rocks.mygiftlist.ion.schema :as schema]
   [rocks.mygiftlist.model.user :as user]))

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
  (def db-name "my-db")
  (d/create-database client {:db-name db-name})
  (def conn (d/connect client {:db-name db-name}))
  (d/transact conn {:tx-data schema/schema})
  (d/transact conn {:tx-data [#::user {:id (java.util.UUID/randomUUID)
                                       :auth0-id "auth0|5dc81bfc1658c30e5fe9b877"
                                       :email "bob@example.com"
                                       :created-at (java.util.Date.)}]})
  (d/q '{:find [(pull ?u [::user/id ::user/auth0-id ::user/email])]
         :where [[?u ::user/id]]}
    (get-db))
  (get-user-by-auth0-id [::user/id ::user/auth0-id ::user/email ::user/given-name ::user/family-name]
    "auth0|5dc81bfc1658c30e5fe9b877")
  )
