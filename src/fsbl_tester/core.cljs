(ns fsbl-tester.core
  (:require [reagent.core :as reagent :refer [atom]]
            [fsbl-tester.view :refer [view]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; define your app data so that it doesn't get over-written on reload
(comment
  (defonce app-state (atom {:dirty? false}))

  (def ws (.. js/FSBL -Clients -WorkspaceClient))
  (def rc (.. js/FSBL -Clients -RouterClient))
  (def hk (.. js/FSBL -Clients -HotkeyClient))

  (.addGlobalHotkey hk #js ["ctrl" "shift" "s"] #(do (prn "saving") (.save ws)))

  (.subscribe rc "Finsemble.WorkspaceService.update"
              #(swap! app-state assoc :dirty? (.. %2 -data -activeWorkspace -isDirty))))


(defn start []
  (reagent/render-component [view]
                            (. js/document (getElementById "app"))))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))
