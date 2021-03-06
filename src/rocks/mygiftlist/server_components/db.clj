(ns rocks.mygiftlist.server-components.db
  (:require [rocks.mygiftlist.config :as config]
            [mount.core :refer [defstate]]
            [hikari-cp.core :as pool]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]
            [next.jdbc.prepare :as p]
            [clojure.string :as str]
            [honeysql.core :as sql]
            [honeysql.format :as sqlf]
            honeysql-postgres.format))

;; TODO: Look into `:reWriteBatchedInserts`
(def datasource-options
  (merge {:auto-commit        true
          :read-only          false
          :connection-timeout 30000
          :validation-timeout 5000
          :idle-timeout       600000
          :max-lifetime       1800000
          :minimum-idle       10
          :maximum-pool-size  10
          :pool-name          "db-pool"
          :adapter            "postgresql"
          :register-mbeans    false}
    config/database-spec))

(defstate pool
  :start (pool/make-datasource datasource-options)
  :stop (pool/close-datasource pool))

(defn- qualify [table]
  (str "rocks.mygiftlist.type."
    (case table
      "invitation" "gift-list.invitation"
      "invitation-acceptance" "gift-list.invitation-acceptance"
      "revocation" "gift-list.revocation"
      table)))

(defn as-qualified-kebab-maps [rs opts]
  (let [kebab #(str/replace % #"_" "-")]
    (result-set/as-modified-maps rs (assoc opts
                                      :qualifier-fn (comp qualify kebab)
                                      :label-fn kebab))))

(def query-opts {:builder-fn as-qualified-kebab-maps})

(defn execute! [pool sql-map]
  (jdbc/execute! pool
    (sql/format sql-map :quoting :ansi)
    query-opts))

(defn execute-one! [pool sql-map]
  (jdbc/execute-one! pool
    (sql/format sql-map :quoting :ansi)
    query-opts))

(extend-protocol result-set/ReadableColumn

  ;; Automatically convert java.sql.Array into clojure vector
  java.sql.Array
  (read-column-by-label ^java.sql.Array [^java.sql.Array v _]
    (into [] (.getArray v)))
  (read-column-by-index ^java.sql.Array [^java.sql.Array v _2 _3]
    (into [] (.getArray v)))

  ;; Output java.time.LocalDate instead of java.sql.Date in query results
  java.sql.Date
  (read-column-by-label ^java.time.LocalDate [^java.sql.Date v _]
    (.toLocalDate v))
  (read-column-by-index ^java.time.LocalDate [^java.sql.Date v _2 _3]
    (.toLocalDate v))

  ;; Output java.time.Instant instead of java.sql.Timestamp in query results
  java.sql.Timestamp
  (read-column-by-label ^java.time.Instant [^java.sql.Timestamp v _]
    (.toInstant v))
  (read-column-by-index ^java.time.Instant [^java.sql.Timestamp v _2 _3]
    (.toInstant v)))


(extend-protocol p/SettableParameter

  ;; Accept java.time.Instant as a query param
  java.time.Instant
  (set-parameter [^java.time.Instant v ^java.sql.PreparedStatement ps ^long i]
    (.setTimestamp ps i (java.sql.Timestamp/from v)))

  ;; Accept java.time.LocalDate as a query param
  java.time.LocalDate
  (set-parameter [^java.time.LocalDate v ^java.sql.PreparedStatement ps ^long i]
    (.setTimestamp ps i (java.sql.Timestamp/valueOf (.atStartOfDay v)))))

(defmethod sqlf/fn-handler "array_agg_distinct"
  [_ a]
  (format "array_agg(DISTINCT %s)" (sqlf/to-sql a)))

(comment
  (mount.core/start)
  (jdbc/execute! pool ["select * from \"user\""] {:builder-fn as-qualified-kebab-maps})
  (mount.core/stop)
  (parse-url  "postgresql://postgres:@localhost:15432/postgres")
  (zipmap
    [:username :password :server-name :port-number :database-name]
    (drop 1 (re-find #"postgresql://(.+):(.*)@(.+):(\d+)/(.+)" "postgresql://postgres:@localhost:15432/postgres")))
  (sql/format {:insert-into :foo
               :values [{::config/id "id"
                         ::config/full-name "name"}]}
    :quoting :ansi)
  )
