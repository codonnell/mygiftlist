(ns rocks.mygiftlist.ui.navigation
  (:require [rocks.mygiftlist.auth :as auth]
            [rocks.mygiftlist.routing :as routing]
            [rocks.mygiftlist.type.user :as user]
            [rocks.mygiftlist.type.gift-list :as gift-list]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu :refer [ui-menu]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-menu :refer [ui-menu-menu]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-header :refer [ui-menu-header]]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-item :refer [ui-menu-item]]
            [goog.string :as gs]
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

(defsc CreatedGiftListItem [this {::gift-list/keys [name]}]
  {:query [::gift-list/id ::gift-list/name]
   :ident ::gift-list/id}
  (ui-menu-item {:active false
                 :onClick #(js/console.log "Click" name)}
    (dom/div {} name)))

(def ui-created-gift-list-item (comp/factory CreatedGiftListItem {:keyfn ::gift-list/id}))

(defsc InvitedGiftListAuthor [this {::user/keys [given-name family-name email]}]
  {:query [::user/id ::user/given-name ::user/family-name ::user/email]
   :ident ::user/id}
  (dom/div {}
    (if (and given-name family-name)
      (gs/format "%s %s (%s)" given-name family-name email)
      email)))

(def ui-invited-gift-list-author (comp/factory InvitedGiftListAuthor))

(defsc InvitedGiftListItem [this {::gift-list/keys [name created-by]}]
  {:query [::gift-list/id ::gift-list/name {::gift-list/created-by (comp/get-query InvitedGiftListAuthor)}]
   :ident ::gift-list/id}
  (ui-menu-item {:active false
                 :onClick #(js/console.log "Click" name)}
    (dom/div name)
    (ui-invited-gift-list-author created-by)))

(def ui-invited-gift-list-item (comp/factory InvitedGiftListItem {:keyfn ::gift-list/id}))

(defsc LeftNav [this {:keys [created-gift-lists invited-gift-lists] :as props}]
  {:query [{:created-gift-lists (comp/get-query CreatedGiftListItem)}
           {:invited-gift-lists (comp/get-query InvitedGiftListItem)}]
   :ident (fn [] [:component/id :left-nav])
   :initial-state {:created-gift-lists []
                   :invited-gift-lists []}}
  (ui-menu {:vertical true :className "leftnav menu"}
    (ui-menu-item {}
      (ui-menu-header {} "My Lists")
      (ui-menu-menu {}
        (mapv ui-created-gift-list-item created-gift-lists)
        #_(ui-menu-item {:active false
                       :onClick #(js/console.log "Nav birthday 2019")}
          (dom/div "Birthday 2019"))
        #_(ui-menu-item {:active false
                       :onClick #(js/console.log "Nav christmas 2019")}
          (dom/div "Christmas 2019"))))
    (ui-menu-item {}
      (ui-menu-header {} "Invited Lists")
      (ui-menu-menu {}
        (mapv ui-invited-gift-list-item invited-gift-lists)
        #_(ui-menu-item {:active false
                       :onClick #(js/console.log "Nav birthday 2019")}
          (dom/div "Birthday 2019")
          (dom/div "- Marty McFly"))
        #_(ui-menu-item {:active false
                       :onClick #(js/console.log "Nav christmas 2019")}
          (dom/div "Christmas 2019")
          (dom/div "- marty@example.com"))))))

(def ui-left-nav (comp/factory LeftNav))
