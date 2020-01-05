(ns rocks.mygiftlist.server-components.pathom
  (:require
   [taoensso.timbre :as log]
   [mount.core :refer [defstate]]
   [com.wsscode.pathom.connect :as pc]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.common.async-clj :refer [let-chan]]
   [clojure.core.async :as async]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.type.gift :as gift]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.model.user :as model.user]
   [rocks.mygiftlist.model.gift :as model.gift]
   [rocks.mygiftlist.model.gift-list :as model.gift-list]
   [rocks.mygiftlist.server-components.db :as db]))

(pc/defresolver index-explorer [env _]
  {::pc/input  #{:com.wsscode.pathom.viz.index-explorer/id}
   ::pc/output [:com.wsscode.pathom.viz.index-explorer/index]}
  {:com.wsscode.pathom.viz.index-explorer/index
   (-> (get env ::pc/indexes)
     (update ::pc/index-resolvers #(into {} (map (fn [[k v]] [k (dissoc v ::pc/resolve)])) %))
     (update ::pc/index-mutations #(into {} (map (fn [[k v]] [k (dissoc v ::pc/mutate)])) %)))})

(def all-resolvers [index-explorer
                    model.user/user-resolvers
                    model.gift/gift-resolvers
                    model.gift-list/gift-list-resolvers])

(defn preprocess-parser-plugin
  "Helper to create a plugin that can view/modify the env/tx of a top-level request.
  f - (fn [{:keys [env tx]}] {:env new-env :tx new-tx})
  If the function returns no env or tx, then the parser will not be called (aborts the parse)"
  [f]
  {::p/wrap-parser
   (fn transform-parser-out-plugin-external [parser]
     (fn transform-parser-out-plugin-internal [env tx]
       (let [{:keys [env tx] :as req} (f {:env env :tx tx})]
         (if (and (map? env) (seq tx))
           (parser env tx)
           {}))))})

(defn log-requests [{:keys [env tx] :as req}]
  (log/debug "Pathom transaction:" (pr-str tx))
  req)

(defstate parser
  :start
  (let [real-parser (p/parallel-parser
                      {::p/mutate  pc/mutate-async
                       ::p/env     {::p/reader               [p/map-reader pc/parallel-reader
                                                              pc/open-ident-reader p/env-placeholder-reader]
                                    ::p/placeholder-prefixes #{">"}}
                       ::p/plugins [(pc/connect-plugin {::pc/register all-resolvers})
                                    (p/env-wrap-plugin (fn [env]
                                                         ;; Here is where you can dynamically add things to the resolver/mutation
                                                         ;; environment, like the server config, database connections, etc.
                                                         (assoc env
                                                           :requester-auth0-id (get-in env [:ring/request :claims :sub])
                                                           ::db/pool db/pool)))
                                    (preprocess-parser-plugin log-requests)
                                    p/error-handler-plugin
                                    p/request-cache-plugin
                                    (p/post-process-parser-plugin p/elide-not-found)
                                    p/trace-plugin]})
        ;; NOTE: Add -Dtrace to the server JVM to enable Fulcro Inspect query performance traces to the network tab.
        ;; Understand that this makes the network responses much larger and should not be used in production.
        trace?      (not (nil? (System/getProperty "trace")))]
    (fn wrapped-parser [env tx]
      (async/<!! (real-parser env (if trace?
                                    (conj tx :com.wsscode.pathom/trace)
                                    tx))))))

(comment
  (parser {:ring/request {:claims {:sub "auth0|abcd1234" #_"auth0|5dc81bfc1658c30e5fe9b877"}}}
    #_[{[::gift-list/id #uuid "df687d54-c716-4fcc-9f88-03f4fee90209"]
      [::gift-list/name ::gift-list/created-at
       {::gift-list/created-by [::user/id]}
       {::gift-list/gifts [::gift/id]}]}]
    #_[{:created-gift-lists [::gift-list/id ::gift-list/name
                           {::gift-list/created-by [::user/id ::user/auth0-id]}
                           {::gift-list/gifts [::gift/id ::gift/name ::gift/description]}]}
     {:invited-gift-lists [::gift-list/id ::gift-list/name]}]
    #_[{[:component/id :left-nav]
      [{:created-gift-lists [::gift-list/id ::gift-list/name]}
       {:invited-gift-lists
        [::gift-list/id ::gift-list/name
         {::gift-list/created-by
          [::user/id ::user/given-name ::user/family-name ::user/email]}]}]}]
    [{[::user/auth0-id "auth0|abcd1234"]
      [::user/id ::user/given-name ::user/email
       {::user/created-gift-lists [::gift-list/id ::gift-list/name]}]}]
    #_[{:created-gift-lists
      [::gift-list/id ::gift-list/name
       {::gift-list/created-by
        [::user/id ::user/given-name ::user/family-name ::user/email]}]}
     {:invited-gift-lists
      [::gift-list/id ::gift-list/name
       {::gift-list/created-by
        [::user/id ::user/given-name ::user/family-name ::user/email]}]}]
    #_[{[::gift-list/id #uuid "0ebaf3ee-7d0d-4573-880a-ad2cb8582ec7"]
      [::gift-list/id ::gift-list/name ::gift-list/created-at
       {::gift-list/gifts
        [::gift/id ::gift/name ::gift/description]}]}])
  )
