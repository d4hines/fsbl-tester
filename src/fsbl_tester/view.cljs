(ns fsbl-tester.view
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn view [state]
  [:div
   [:h1 "Hello world!"]])