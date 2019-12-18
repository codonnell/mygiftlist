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

(defonce auth0-client (atom nil))

(defn create-auth0-client! []
  (go (reset! auth0-client (<!p (create-auth0-client #js {:domain config/AUTH0_DOMAIN
                                                          :client_id config/AUTH0_CLIENT_ID
                                                          :audience config/AUTH0_AUDIENCE})))))

;; TODO: Fold this in to app state
(def authenticated (atom false))

(defn is-authenticated? []
  (go (let [authenticated? (<!p (.isAuthenticated @auth0-client))]
        (reset! authenticated authenticated?)
        (js/console.log "Authenticated" authenticated?))))

(defn login []
  (go (<!p (.loginWithRedirect @auth0-client
             #js {:redirect_uri (.. js/window -location -origin)}))))

(defn handle-redirect-callback []
  (go (<!p (.handleRedirectCallback @auth0-client))))

(defn logout []
  (go (.logout @auth0-client #js {:returnTo (.. js/window -location -origin)})))

(defn get-access-token []
  (go (<!p (.getTokenSilently @auth0-client))))

(defn get-user-info []
  (go (<!p (.getUser @auth0-client))))
