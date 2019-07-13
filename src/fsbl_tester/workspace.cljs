(ns fsbl-tester.workspace
  #:ghostwheel.core{:check true
                    :num-tests 10}
  (:require [clojure.spec.gen.alpha :as gen :refer [generate]]
            [clojure.spec.alpha :as s :refer [spec]]))

(defn gen [sp] (generate (s/gen sp)))

(s/def ::name (s/and string? (partial not= "")))
(s/def ::windowNames (s/coll-of ::name))
(s/def ::isMovable boolean?)
(s/def ::group (s/keys :req-un [::isMovable ::windowNames]))
(s/def ::groups (s/map-of ::name ::group))
(s/def ::linkerGroup #{:group1 :group2 :group3 :group4 :group5 :group6})
(s/def ::Finsemble_Linker (s/map-of ::linkerGroup boolean?))

(s/def ::componentState (s/nilable (s/keys :opt-un [::Finsemble_Linker])))
(s/def ::componentStates (s/map-of string? ::componentState))

(s/def ::componentType #{"Welcome Component" "Notepad" "CLJSComponent"})

(s/def ::bound (s/nilable (s/int-in 0 2000)))
(s/def ::defaultLeft ::bound)
(s/def ::defaultHeight ::bound)
(s/def ::defaultTop ::bound)
(s/def ::defaultWidth ::bound)
(s/def ::windowName ::name)
(s/def ::childWindowIdentifier (s/keys :req-un [::windowName]))
(s/def ::childWindowIdentifiers (s/coll-of ::childWindowIdentifier))
(s/def :stackedWindow/componentType #{"StackedWindow"})

(s/def ::window (s/or :normalWindow (s/keys :req-un [::componentType
                                                     ::name]
                                            :opt-un [::defaultHeight
                                                     ::defaultLeft
                                                     ::defaultTop
                                                     ::defaultWidth])
                      :stackedWindow (s/keys :req-un [::name
                                                      ::childWindowIdentifiers
                                                      :stackedWindow/componentType])))
(s/def ::windowData (s/coll-of ::window))
(s/def ::windows (s/coll-of ::name))

(s/def ::version #{"1.0.0"})
(s/def ::type #{"workspace"})
(s/def :workspace/name string?)

(s/def ::workspace (s/keys :req-un [:workspace/name
                                    ::version
                                    ::type
                                    ::groups
                                    ::windowData
                                    ::windows]))
