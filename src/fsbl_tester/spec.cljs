(ns fsbl-tester.spec
  (:require [clojure.spec.alpha :as s :refer [spec]]
            [clojure.spec.gen.alpha :as gen :refer [generate]]
            [clojure.test.check.generators]
            [cljs.pprint :refer [pprint]]
            [meander.strategy.delta :as r :include-macros true]
            [expound.alpha]))

(set! s/*explain-out* expound.alpha/printer)
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
             :Stack1 {:windowData {:childWindowIdentifiers #{"Bar" "Foo"}
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

(s/def ::name (s/and string? (partial not= "")))
(s/def ::groups (s/map-of ::name (s/coll-of ::name :kind set?)))
(s/def ::linkerGroup #{:group1 :group2 :group3 :group4 :group5 :group6})
(s/def ::Finsemble_Linker (s/map-of ::linkerGroup boolean?))
(s/def ::componentState (s/keys :opt-un [::Finsemble_Linker]))

(s/def ::componentType #{"Welcome Component" "WPFExample" "Notepad"})

(s/def ::bound (s/int-in 0 2000))
(s/def ::defaultLeft ::bound)
(s/def ::defaultHeight ::bound)
(s/def ::defaultTop ::bound)
(s/def ::defaultWidth ::bound)
(s/def ::childWindowIdentifiers (s/coll-of ::name :kind set?))
(s/def :stackedWindow/componentType #{"StackedWindow"})

(s/def ::windowData (s/or :normalWindow (s/keys :req-un [::componentType]
                                                :opt-un [::defaultHeight
                                                         ::defaultLeft
                                                         ::defaultTop
                                                         ::defaultWidth])
                          :stackedWindow (s/keys :req-un [::childWindowIdentifiers :stackedWindow/componentType])))
(s/def ::window (s/keys :req-un [::windowData] :opt-un [::componentState]))
(s/def ::windows (s/map-of ::name ::window))
(s/def ::version #{"1.0.0"})
(s/def ::type #{"workspace"})
(s/def :workspace/name string?)

(s/def ::workspace (s/keys :req-un [:workspace/name
                                    ::version
                                    ::type
                                    ::groups
                                    ::windows]))

(s/def ::add-linker (s/with-gen))
(s/def ::bounds (s/cat :defaultTop ::bound :defaultLeft ::bound :defaultHeight ::bound :defaultWidth ::bound))

(s/def ::new-window (s/cat
                     :componentType ::componentType
                     :name ::name
                     :bounds ::bounds))
(s/conform ::new-window ["Notepad" "46gVtzp" 1097 1995 1529 1344])
(s/def ::new-stack (s/cat
                    :type #{"Stacked Window" "stacked window"
                            "Stack of" "stack of"}
                    :name ::name
                    :children (s/+ ::new-window)))

(s/conform ::new-stack (gen/generate (s/gen ::new-stack)))
(s/def ::new-group (s/cat
                    :type #{"Group" "group"}
                    :name ::name
                    :windows (s/+ (s/or
                                   :stacked-window ::new-stack
                                   :window ::new-window))))
(def foo (s/conform ::new-group (gen/generate (s/gen ::new-group))))


;; I'm trying to convert between these two formats
;; First format


{:type "Group"
 :name "group1"
 :windows [;; A vec of ::windows, made up of ::componentState
           ;; and ::windowData
           {:componentState {:foo :bar}
            :windowData
            {:name "Win1"
             :componentType "Notepad"
             :bounds {:defaultHeight 100
                      :defaultWidth 300}}}]}
;; Second format
{:windows [;; This is  vec of *just* ::windowData
           ;; with the :name added.
           {:name "Win1"
            :componentType "Notepad"
            :bounds {:defaultHeight 100
                     :defaultWidth 300}}]

 ;; The ::componentState is seperate and indexed by name
 :componentState {"Win1" {:foo :bar}}
 :groups {"group1" {:windows ["Win1"]}}}

{:type "Group"
 :name "group1"
 :windows [{:componentState {:foo :bar}
            :windowData
            {:name "Win1"
             :componentType "Notepad"
             :bounds {:defaultLeft 2
                      :defaultHeight 100
                      :defaultTop 200
                      :defaultWidth 300}}}]}

(def w [{:type "Group"
         :name "group1"
         :windows [{:name "Win1"
                    :componentType "Notepad"
                    :bounds {:defaultLeft 2
                             :defaultHeight 100
                             :defaultTop 200
                             :defaultWidth 300}}]}
        {:type "Group"
         :name "group2"
         :windows [{:name "Win1"
                    :componentType "Notepad"
                    :bounds {:defaultLeft 2
                             :defaultHeight 100
                             :defaultTop 200
                             :defaultWidth 300}}]}])

"
?Component ?Name (with bounds ?bounds)* (on linker channel ?channel)
Group ?Name with windows ?Windows
Stacked Window ?Name 
"
"
Welcome Component A with boudns 0 0 500 500 on linker channel green

"