(ns fsbl-tester.api
  #:ghostwheel.core{:check true
                    :num-tests 10}
  (:require
   [clojure.spec.alpha :as s :refer [spec]]
   [clojure.spec.gen.alpha :as gen :refer [generate]]
   [fsbl-tester.workspace :as ws]
   [fsbl-tester.parse :as p]
   [ghostwheel.core :as g
    :refer [>defn => | <- ?]]))

(def wsc (.. js/FSBL -Clients -WorkspaceClient))
(def log (.log js/console))

(>defn stringify-keys
       [my-map]
       [(s/map-of keyword? any?) => (s/map-of string? any?)]
       (reduce (fn [m [k v]] (assoc m (name k) v))
               {} my-map))

(defn export! [name]
  (->
   (.export wsc #js {"workspaceName" name})
   (.then #(-> %
               (js->clj :keywordize-keys true)
               (get (keyword name))
               (update :componentStates stringify-keys)
               (update :groups stringify-keys)))))

(-> (export! "Default Workspace")
    (.then ))
(>defn import-ws!
       [ws]
       [::ws/workspace => nil?]
       (s/conform ::ws/workspace ws)
       (.import wsc #js {:workspaceJSONDefinition
                         (clj->js {(:name ws) ws})
                         :force true}))

(def ws (atom nil))

(comment
 (-> (p/parse
      "
Stacked Window S4 of Welcome Component A1, Welcome Component B1")
     (p/transform )
     (assoc :name "ding2")
     import-ws!)
 (.then (export "Default Workspace")
         #(import-ws (assoc % :name "bar"))))
