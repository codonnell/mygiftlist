(ns rocks.mygiftlist.client
  (:require [rocks.mygiftlist.application :refer [SPA]]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.semantic-ui.elements.header.ui-header :refer [ui-header]]
            [taoensso.timbre :as log]))

(defsc Root [this _props]
  {:query []
   :initial-state {}}
  (ui-header {:as "h1"} "Hello World"))

(defn ^:export refresh []
  (log/info "Hot code reload...")
  (app/mount! SPA Root "app"))

(defn ^:export init []
  (log/info "Application starting...")
  (app/mount! SPA Root "app"))
