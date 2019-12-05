(ns rocks.mygiftlist.auth
  (:require ["react" :refer [useContext useState createContext useEffect createElement]]
            ["@auth0/auth0-spa-js" :as create-auth0-client]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.wsscode.common.async-cljs :refer [go-promise <!p]]
            [clojure.string :as str]
            [rocks.mygiftlist.routing :as routing]
            [taoensso.timbre :as log]))

(defn default-redirect-callback []
  (routing/route-to! (.. js/window -location -pathname)))

(def auth0-context (createContext))
(defn use-auth0 []
  (useContext auth0-context))

(defn auth0-provider
  [init-options]
  (when-not (.-onRedirectCallback init-options)
    (aset init-options "onRedirectCallback" default-redirect-callback))
  (let [onRedirectCallback (.-onRedirectCallback init-options)
        [is-authenticated set-authenticated] (useState)
        [user set-user] (useState)
        [auth0-client set-auth0] (useState)
        [loading set-loading] (useState true)
        [popup-open set-popup-open] (useState false)]
    (useEffect
      (fn []
        (go-promise
          (let [auth0-from-hook (<!p (create-auth0-client (-> init-options
                                                            js->clj
                                                            (dissoc :children)
                                                            clj->js)))
                _ (set-auth0 auth0-from-hook)
                _ (when (str/includes? (.. js/window -location -search) "code=")
                    (let [ret (<!p (.handleRedirectCallback auth0-from-hook))]
                      (onRedirectCallback (.-appState ret))))
                is-authenticated* (<!p (.isAuthenticated auth0-from-hook))]
            (set-authenticated is-authenticated*)
            (when is-authenticated*
              (let [user* (<!p (.getUser auth0-from-hook))]
                (set-user user*)))
            (set-loading false))
          ::success)
        js/undefined)
      #js [])
    (let [login-with-popup
          (fn [params]
            (go-promise
              (set-popup-open true)
              (<!p (.loginWithPopup auth0-client params))
              (set-popup-open false)
              (let [user* (<!p (.getUser auth0-client))]
                (set-user user*)
                (set-authenticated true))
              ::success))
          handle-redirect-callback
          (fn []
            (go-promise
              (set-loading true)
              (<!p (.handleRedirectCallback auth0-client))
              (let [user* (<!p (.getUser auth0-client))]
                (set-loading false)
                (set-authenticated true)
                (set-user user*))
              ::success))]
      (apply createElement (.-Provider auth0-context)
        #js {:value #js {:isAuthenticated is-authenticated
                         :user user
                         :loading loading
                         :popupOpen popup-open
                         :loginWithPopup login-with-popup
                         :handleRedirectCallback handle-redirect-callback
                         :getIdTokenClaims (fn [& args] (apply (.-getIdTokenClaims auth0-client) args))
                         :loginWithRedirect (fn [& args] (apply (.-loginWithRedirect auth0-client) args))
                         :getTokenSilently (fn [& args] (apply (.-getTokenSilently auth0-client) args))
                         :getTokenWithPopup (fn [& args] (apply (.-getTokenWithPopup auth0-client) args))
                         :logout (fn [& args] (apply (.-logout auth0-client) args))}}
        (.-children init-options)))))
