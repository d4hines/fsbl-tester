(ns fsbl-tester.view
  #:ghostwheel.core{:check true}
  (:require
   [reagent.core :as reagent :refer [atom]]
   [ghostwheel.core :as g
    :refer [>defn >defn- >fdef => | <- ?]]))

(defonce dirty? (atom true))

(defn view []
  [:div
   [:h1 {:style {:color
                 ({true "red" false "purple"}
                  @dirty?)}}
    (str "The workspace is: " @dirty?)]])

(def rc (.. js/FSBL -Clients -RouterClient))
(.subscribe rc "Finsemble.WorkspaceService.update"
            #(reset! dirty? (.. %2 -data -activeWorkspace -isDirty)))
