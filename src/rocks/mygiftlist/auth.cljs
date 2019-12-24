(ns rocks.mygiftlist.auth
  (:require ["@auth0/auth0-spa-js" :as create-auth0-client]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
            [com.wsscode.common.async-cljs :refer [go-promise <!p]]
            [clojure.core.async :refer [go <!]]
            [clojure.string :as str]
            [rocks.mygiftlist.config :as config]
            [taoensso.timbre :as log]))

(defonce auth0-client (atom nil))

(defn create-auth0-client! []
  (go (reset! auth0-client (<!p (create-auth0-client #js {:domain config/AUTH0_DOMAIN
                                                          :client_id config/AUTH0_CLIENT_ID
                                                          :audience config/AUTH0_AUDIENCE
                                                          :connection config/AUTH0_CONNECTION})))))

(defn is-authenticated? []
  (go (<!p (.isAuthenticated @auth0-client))))

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

(defmutation set-current-user
  [user]
  (action [{:keys [state]}]
    (swap! state assoc-in [:component/id :current-user] user)))
