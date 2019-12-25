(ns rocks.mygiftlist.ion.query
  (:require [com.wsscode.pathom.connect :as pc]))

(defmacro batch-query-by [k query inputs]
  `(->> ~inputs
     (mapv ~k)
     ~query
     (mapv first)
     (pc/batch-restore-sort {::pc/inputs ~inputs ::pc/key ~k})))

(comment
  (batch-query-by ::id (d/q '{:find [?id]
                              :in [$ [auth0-id ...]]
                              :where [[?u ::id ?id]
                                      [?u ::auth0-id ?auth0-id]]}
                         db) ["auth0|abc" "auth0|def"])
  )
