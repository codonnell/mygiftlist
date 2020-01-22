(ns rocks.mygiftlist.client
  (:require [rocks.mygiftlist.application :refer [SPA]]
            [rocks.mygiftlist.auth :as auth]
            [rocks.mygiftlist.model.user :as model.user]
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
        (comp/transact! SPA [{(model.user/upsert-user-on-auth0-id {::user/auth0-id sub ::user/email email})
                              (comp/get-query ui.nav/CurrentUser)}])
        (routing/restore!)
        (df/load! SPA [:component/id :left-nav] ui.nav/LeftNav))
      (do
        (routing/save!)
        (comp/transact! SPA [(routing/route-to {:path "/login"})])))))

(comment
  (comp/get-query ui.nav/CurrentUser)
  )
