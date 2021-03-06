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
   [rocks.mygiftlist.model.gift-list.invitation :as model.gift-list.invitation]
   [rocks.mygiftlist.server-components.db :as db]
   [rocks.mygiftlist.type.gift-list.invitation :as invitation]))

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
                    model.gift-list/gift-list-resolvers
                    model.gift-list.invitation/invitation-resolvers])

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
                                    ::p/process-error (fn [env e]
                                                        (log/error (p/error-str e))
                                                        (log/error (with-out-str (.printStackTrace e)))
                                                        {::p/error (ex-data e)})
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
  (mount.core/start)
  (parser {:ring/request {:claims {:sub "auth0|5e65ad3bc6dbc90d3de4fa12" #_"auth0|abcd1234" #_"auth0|5dfeec6f9567eb0dc0302207"}}}
    `[{[::invitation/id #uuid "8e8544d2-2ea4-4e20-85d2-8bf52c264b94"]
       [{::invitation/gift-list [::gift-list/id]}]}
      {(model.gift-list.invitation/accept-invitation
         ~{::invitation/token "qTqnXPBVx6iMzq71JaHTAL"})
       [{::invitation/gift-list [::gift-list/id]}]}]
    #_[{:created-invitations [::invitation/id ::invitation/token]}]
    #_[{[::invitation/id #uuid "10f3b1fd-1ca5-418d-b554-feffd0a2a159"]
      [::invitation/id ::invitation/token ::invitation/created-at ::invitation/expires-at
       {::invitation/created-by [::user/id ::user/auth0-id ::user/email]}
       {::invitation/gift-list [::gift-list/id ::gift-list/name]}]}]
    #_`[(model.gift-list.invitation/create-invitation
        {::gift-list/id #uuid "0dc57c27-78a4-46ba-9997-6113c529b8bc"})]
    #_[{[:component/id :left-nav]
      [{:created-gift-lists
        [:rocks.mygiftlist.type.gift-list/id
         :rocks.mygiftlist.type.gift-list/name]}
       {:invited-gift-lists
        [:rocks.mygiftlist.type.gift-list/id
         :rocks.mygiftlist.type.gift-list/name
         {:rocks.mygiftlist.type.gift-list/created-by
          [:rocks.mygiftlist.type.user/id
           :rocks.mygiftlist.type.user/given-name
           :rocks.mygiftlist.type.user/family-name
           :rocks.mygiftlist.type.user/email]}]}]}]
    #_`[{(model.user/upsert-user-on-auth0-id #::user {:auth0-id "auth0|abcd1234"
                                                    :email "fake2@example.com"})
       [::user/id ::user/email ::user/auth0-id ::user/given-name ::user/family-name]}]
    #_[{[::gift-list/id #uuid "df687d54-c716-4fcc-9f88-03f4fee90209"]
      [::gift-list/name ::gift-list/created-at
       {::gift-list/created-by [::user/id]}
       {::gift-list/gifts [::gift/id]}]}]
    #_[{:created-gift-lists [::gift-list/id ::gift-list/name
                           {::gift-list/created-by [::user/id ::user/auth0-id]}
                           {::gift-list/gifts [::gift/id ::gift/name ::gift/description]}]}
     {:invited-gift-lists [::gift-list/id ::gift-list/name]}])
  )
