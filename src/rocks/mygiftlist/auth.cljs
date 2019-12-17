(ns rocks.mygiftlist.auth
  (:require ["react" :refer [useContext useState createContext useEffect createElement]]
            ["@auth0/auth0-spa-js" :as create-auth0-client]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.wsscode.common.async-cljs :refer [go-promise <!p]]
            [clojure.core.async :refer [go]]
            [clojure.string :as str]
            [rocks.mygiftlist.routing :as routing]
            [rocks.mygiftlist.config :as config]
            [taoensso.timbre :as log]))

(def auth0-atom (atom nil))

(defn create-auth0-client! []
  (go (reset! auth0-atom (<!p (create-auth0-client #js {:domain config/AUTH0_DOMAIN
                                                        :client_id config/AUTH0_CLIENT_ID})))))

(defonce auth0-client (create-auth0-client #js {:domain config/AUTH0_DOMAIN
                                                :client_id config/AUTH0_CLIENT_ID}))

(def authenticated (atom false))

(defn is-authenticated? []
  (go (let [authenticated? (<!p (.isAuthenticated (<!p auth0-client)))]
        (reset! authenticated authenticated?)
        (js/console.log "Authenticated" authenticated?))))

(defn login []
  (go (<!p (.loginWithRedirect (<!p auth0-client)
             #js {:redirect_uri (.. js/window -location -origin)}))))

(defn handle-redirect-callback []
  (go (<!p (.handleRedirectCallback (<!p auth0-client)))))

(defn logout []
  (go (.logout (<!p auth0-client) #js {:returnTo (.. js/window -location -origin)})))

(defn get-access-token []
  (go (<!p (.getTokenSilently (<!p auth0-client)))))

(defn get-user-info []
  (go (<!p (.getUser (<!p auth0-client)))))
