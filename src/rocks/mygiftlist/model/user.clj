(ns rocks.mygiftlist.model.user
  (:require
   [datomic.client.api :as d]
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]))


(defresolver all-users-resolver [{:keys [db]} input]
  {::pc/output [{:all-users [::id]}]}
  {:all-users (->> db
                (d/q '{:find [?id]
                       :where [[_ ::id ?id]]})
                (mapv (fn [[id]] {::id id})))})

(defresolver user-resolver [{:keys [db]} {::keys [id]}]
  {::pc/input #{::id}
   ::pc/output [::email ::auth0-id]}
  (ffirst (d/q '{:find [(pull ?u [::email ::auth0-id])]
                 :in [$ ?id]
                 :where [[?u ::id ?id]]}
            db id)))

(comment
  )
