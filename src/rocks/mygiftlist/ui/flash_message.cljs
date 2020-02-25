(ns rocks.mygiftlist.ui.flash-message
  (:require
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.semantic-ui.collections.message.ui-message :refer [ui-message]]
   [clojure.string :as str]))

(defmutation close [_]
  (action [{:keys [state]}]
    (swap! state assoc-in [:component/id :flash-message]
      {:ui/active false})))

(defsc FlashMessage [this {:ui/keys [message type active]}]
  {:query [:ui/active :ui/message :ui/type]
   :ident (fn [] [:component/id :flash-message])
   :initial-state {:ui/active false}}
  (when active
    (ui-message
      {:className (str/join " " [(name type) "mgl_flash-message"])
       :onDismiss #(comp/transact! this [(close {})])}
      (dom/p message))))

(def ui-flash-message (comp/factory FlashMessage))
