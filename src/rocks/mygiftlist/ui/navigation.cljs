(ns rocks.mygiftlist.ui.navigation
  (:require [rocks.mygiftlist.auth :as auth]
            [rocks.mygiftlist.routing :as routing]
            [rocks.mygiftlist.type.user :as user]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu :refer [ui-menu]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-menu :refer [ui-menu-menu]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-header :refer [ui-menu-header]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-item :refer [ui-menu-item]]
            ))

(defsc CurrentUser [this {::user/keys [id email] :as user}]
  {:query [::user/id ::user/email]
   :ident (fn [] [:component/id :current-user])
   :initial-state {}}
  (if (seq user)
    (ui-menu-item {:name "logout"
                   :active false
                   :onClick #(auth/logout)})
    (ui-menu-item {:active false
                   :onClick #(auth/login)}
      "Login/Signup")))

(def ui-current-user (comp/factory CurrentUser))

(defsc Navbar [this {:keys [current-user]}]
  {:query [{:current-user (comp/get-query CurrentUser)}]
   :ident (fn [] [:component/id :navbar])
   :initial-state {:current-user {}}}
  (let [logged-in (seq current-user)]
    (ui-menu {:secondary true}
      (ui-menu-item {:name "home"
                     :active false
                     :onClick #(comp/transact! this [(routing/route-to {:route-string (if logged-in "/home" "/login")})])})
      (ui-menu-menu {:position "right"}
        (ui-current-user current-user)))))

(def ui-navbar (comp/factory Navbar))

(defsc LeftNav [this _]
  {:query []
   :ident (fn [] [:component/id :left-nav])
   :initial-state {}}
  (ui-menu {:vertical true :className "leftnav menu"}
    (ui-menu-item {}
      (ui-menu-header {} "My Lists")
      (ui-menu-menu {}
        (ui-menu-item {:active false
                       :onClick #(js/console.log "Nav birthday 2019")}
          (dom/div "Birthday 2019"))
        (ui-menu-item {:active false
                       :onClick #(js/console.log "Nav christmas 2019")}
          (dom/div "Christmas 2019"))))
    (ui-menu-item {}
      (ui-menu-header {} "Shared Lists")
      (ui-menu-menu {}
        (ui-menu-item {:active false
                       :onClick #(js/console.log "Nav birthday 2019")}
          (dom/div "Birthday 2019")
          (dom/div "- Marty McFly"))
        (ui-menu-item {:active false
                       :onClick #(js/console.log "Nav christmas 2019")}
          (dom/div "Christmas 2019")
          (dom/div "- marty@example.com"))))))

(def ui-left-nav (comp/factory LeftNav))
