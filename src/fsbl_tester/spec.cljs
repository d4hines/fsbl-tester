(ns fsbl-tester.spec
  (:require [clojure.spec.alpha :as s :refer [spec]]
            [spec-tools.data-spec :as ds]
            [spec-tools.core :as stc]))

(def ws-js (.parse js/JSON ws-json))

(js->clj ws-js :keywordize-keys true)

(def ws
  {:version "1.0.0"
   :name "Default Workspace"
   :type "workspace"
   :groups {:group1 #{"WC1" "WC2"}}
   :windows {:WC1 {:componentState {:Finsemble_Linker {:group1 true}}
                   :windowData {:componentType "Welcome Component"
                                :defaultHeight 432
                                :defaultLeft 761
                                :defaultTop 454
                                :defaultWidth 400}}
             :WC2 {:windowData {:componentType "Welcome Component"
                                :defaultHeight 432
                                :defaultLeft 1161
                                :defaultTop 454
                                :defaultWidth 400}}
             :Bar {:windowData {:componentType "Welcome Component"
                                :defaultHeight 432
                                :defaultLeft 1161
                                :defaultTop 454
                                :defaultWidth 400}}
             :Foo {:windowData {:componentType "Welcome Component"
                                :defaultHeight 432
                                :defaultLeft 1161
                                :defaultTop 454
                                :defaultWidth 400}}
             :N {:windowData {:componentType "Notepad"
                              :defaultHeight 432
                              :defaultLeft 1161
                              :defaultTop 454
                              :defaultWidth 400}}
             :WPF {:windowData {:componentType "WPFExample"
                                :defaultHeight 432
                                :defaultLeft 1161
                                :defaultTop 454
                                :defaultWidth 400}}
             :Stack1 {:windowData {:childWindowIdentifiers #{{:windowName "Bar"} {:windowName "Foo"}}
                                   :componentType "StackedWindow"}}}})

(defn kw->string [my-map path]
  (update-in my-map path
             #(reduce-kv
               (fn [m k v]
                 (assoc m (name k) v))
               {} %)))

(def ws-clj (-> ws
                (kw->string [:windows])
                (kw->string [:groups])))

(s/def ::bound (s/int-in 0 1000))
(s/def ::name (s/and string? (partial not= "")))
(s/def ::linkerGroups #{:group1 :group2 :group3 :group4 :group5 :group6})
(s/def ::componentType #{"Welcome Component" "WPFExample" "Notepad"})

(def workspace
  (ds/spec {:version (spec #{"1.0.0"})
            :name string?
            :type (spec #{"workspace"})
            :groups {::name #{::name}}
            :windows {::name {(ds/opt :componentState) {(ds/opt :Finsemble_Linker) {::linkerGroups boolean?}}
                              :windowData (ds/or
                                           {:windowData {:componentType ::componentType
                                                         :defaultHeight ::bound
                                                         :defaultLeft ::bound
                                                         :defaultTop ::bound
                                                         :defaultWidth ::bound}
                                            :stackedWindowData {:childWindowIdentifiers #{{:windowName ::name}}
                                                                :componentType (spec #{"StackedWindow"})}})}}}))


(s/valid? workspace ws-clj)