(ns fsbl-tester.view
  (:require [reagent.core :as reagent :refer [atom]]))
(defonce counter (atom 0))
(defn view []
  [:div
   [:ul
    [:li (str "hi Stephen")]
    [:li (str "The count is " @counter)]]
   [:button {:onClick #(swap! counter (fn [c] (inc c)))} "Increment Counter"]
   [:h1 "Hello CLJS world!"]])

