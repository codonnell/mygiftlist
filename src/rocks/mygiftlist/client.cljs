(ns rocks.mygiftlist.client
  (:require [rocks.mygiftlist.application :refer [SPA]]
            [rocks.mygiftlist.auth :as auth]
            [rocks.mygiftlist.routing :as routing]
            [rocks.mygiftlist.type.user :as user]
            [rocks.mygiftlist.ui.root :as ui.root]
            [rocks.mygiftlist.ui.navigation :as ui.nav]
            [clojure.core.async :refer [go]]
            [clojure.string :as str]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [taoensso.timbre :as log]))

(defn ^:export refresh []
  (log/info "Hot code reload...")
  (app/mount! SPA ui.root/Root "app"))

(defn ^:export init []
  (log/info "Application starting...")
  (go
    (app/mount! SPA ui.root/Root "app")
    (routing/start!)
    (<! (auth/create-auth0-client!))
    (when (str/includes? (.. js/window -location -search) "code=")
      (<! (auth/handle-redirect-callback)))
    (if-let [authenticated (<! (auth/is-authenticated?))]
      (let [{:strs [sub email]} (js->clj (<! (auth/get-user-info)))]
        (comp/transact! SPA [(auth/set-current-user {::user/id sub ::user/email email})
                             (routing/route-to {:route-string "/home"})])
        (df/load! SPA [:component/id :left-nav] ui.nav/LeftNav))
      (comp/transact! SPA [(routing/route-to {:route-string "/login"})]))))

(comment
  )
