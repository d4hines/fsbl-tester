(ns fsbl-tester.view
  #:ghostwheel.core{:check true}
  (:require
   [reagent.core :as reagent :refer [atom]]
   [ghostwheel.core :as g
    :refer [>defn >defn- >fdef => | <- ?]]))

(def state (atom 0))

(defn view []
  [:div
   [:h1 "Hello CLJS world!"]
   [:ul
    [:li (str "The count is " @state)]]
   [:button {:onClick #(swap! state (fn [c] (inc c)))} "Increment Counter"]])

(def rc (.. js/FSBL -Clients -RouterClient))
#_(.subscribe rc "Finsemble.WorkspaceService.update"
              #(swap! state assoc :dirty? (.. %2 -data -activeWorkspace -isDirty)))
