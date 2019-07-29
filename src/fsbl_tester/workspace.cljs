(ns fsbl-tester.workspace
  #:ghostwheel.core{:check true
                    :num-tests 10}
  (:require [clojure.spec.gen.alpha :as gen :refer [generate]]
            [clojure.spec.alpha :as s :refer [spec]]))

(defn gen [sp] (generate (s/gen sp)))

(s/def ::my-seq (s/cat
                 :int int?
                 :string string?
                 :bools (s/* boolean?)))
(s/explain ::my-seq [0 "foo" true true "foo"])
(s/def ::foo string?)

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
(s/def :stackedWindow/windowType #{"StackedWindow"})
(s/def ::window (s/or :normalWindow (s/keys :req-un [::componentType
                                                     ::name]
                                            :opt-un [::defaultHeight
                                                     ::defaultLeft
                                                     ::defaultTop
                                                     ::defaultWidth])
                      :stackedWindow (s/keys :req-un [::name
                                                      ::childWindowIdentifiers
                                                      :stackedWindow/componentType
                                                      :stackedWindow/windowType])))
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

(s/def ::bounds-ast (s/or :empty #{'(nil nil nil nil)}
                          :not-empty (s/cat :defaultTop ::bound
                                 :defaultLeft ::bound
                                 :defaultWidth ::bound
                                 :defaultHeight ::bound)))

(s/def ::window-ast (s/tuple #{:window}
                             ::componentType
                             ::name
                             ::bounds-ast
                             ::componentState))

(s/def ::stackedWindow-pattern (s/cat :type #{:stack}
                                      :name ::name
                                      :first-child ::window-ast
                                      :children (s/+ ::window-ast)))

(s/def ::stackedWindow-ast (s/with-gen (s/and vector? ::stackedWindow-pattern)
                             #(gen/fmap vec (s/gen ::stackedWindow-pattern))))

(s/def ::workspace-pattern (s/cat :init #{:S}
                               :windows (s/* ::window-ast)
                               :stacks (s/* ::stackedWindow-ast)))

(s/def ::workspace-ast (s/with-gen (s/and vector? ::workspace-pattern)
                          #(gen/fmap vec (s/gen ::workspace-pattern))))