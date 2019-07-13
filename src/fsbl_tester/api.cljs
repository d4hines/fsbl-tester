(ns fsbl-tester.api
  #:ghostwheel.core{:check true
                    :num-tests 10}
  (:require
   [clojure.spec.alpha :as s :refer [spec]]
   [clojure.spec.gen.alpha :as gen :refer [generate]]
   [fsbl-tester.workspace :as ws]
   [ghostwheel.core :as g
    :refer [>defn => | <- ?]]))

(def ws (.. js/FSBL -Clients -WorkspaceClient))
(def log (.log js/console))

(>defn stringify-keys
       [my-map]
       [(s/map-of keyword? any?) => (s/map-of string? any?)]
       (reduce (fn [m [k v]] (assoc m (name k) v))
               {} my-map))

(defn export [name]
  (->
   (.export ws #js {"workspaceName" name})
   (.then #(-> %
               (js->clj :keywordize-keys true)
               (get (keyword name))
               (update :componentStates stringify-keys)
               (update :groups stringify-keys)))))

(.then (export "Default Workspace") #(s/explain ::ws/workspace %))