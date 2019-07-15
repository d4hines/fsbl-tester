(ns fsbl-tester.parse
  #:ghostwheel.core{:check true
                    :num-tests 10}
  (:require [fsbl-tester.workspace :as ws :refer [gen]]
            [clojure.string :as str]
            [clojure.spec.alpha :as s :refer [spec]]
            [clojure.spec.gen.alpha :as gen :refer [generate]]
            [instaparse.core :as insta]
            [meander.match.delta :as r.match :include-macros true]
            [meander.substitute.delta :as r.subst]
            [meander.strategy.delta :as r :include-macros true]

            [clojure.test.check.generators]
            [ghostwheel.core :as g
             :refer [>defn => | <- ?]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def ex1  "
Welcome Component FIRST with bounds 100 200 300 400
Stacked Window STACK1 of
  Welcome Component B on linker channel purple
  Welcome Component FOOBAR on linker channel yellow
")

(def color-map {"yellow" :group1
                "purple" :group2})
(>defn parse
       [s]
       [string? => ::ws/workspace-ast]
       (let [cleanStr #(str/replace % #"and|\:" "")
             parser (insta/parser "
S = <'Given'?> window* stack*

name = #'[a-zA-Z0-9]+'
number = #'[0-9]+'

window = component name bounds linkerChannel
component = 'Welcome Component' | 'Notepad'
bounds = (<'with bounds'> number number number number) | ''
linkerChannel = (<'on linker channel'> channel) | ''
channel = 'yellow' | 'purple'

stack = <('Stacked Window' | 'Stack')> name <('with children' | 'of')> window window+
" :auto-whitespace :standard :string-ci true)]
         (->> s cleanStr parser
              (insta/transform {:number int
                                :name identity
                                :channel (fn [c] {:Finsemble_Linker {(get color-map c) true}})
                                :linkerChannel identity
                                :component identity
                                :bounds (fn [& args]
                                          (or args '(nil nil nil nil)))}))))

(defn componentState [expr]
  (into
   {}
   (r.match/search
    expr
    ($ [:window _ !name _ !channel])
    (r.subst/substitute
     {& [[!name !channel] ...]}))))

(defn normalWindows [expr]
  (r.match/search
   expr
   ($ [:window
       ?component
       ?name
       (?top ?left ?width ?height)
       _])
   {:componentType ?component
    :name ?name
    :defaultTop ?top
    :defaultLeft ?left
    :defaultWidth ?width
    :defaultHeight ?height}))

(defn unparse-normalWindows [expr]
  (r.match/search
   expr
   ($ [:window
       ?component
       ?name
       (?top ?left ?width ?height)
       _])
   [?component ?name
    "with bounds" ?top ?left ?width ?height
    ]))

(def win (ws/gen ::ws/window-ast))
(unparse-normalWindows win)
(str/join " " (flatten (unparse-normalWindows win)))

(unparse-normalWindows win)
(defn stackedWindows [expr]
  (r.match/search
   expr
   ($ [:stack
       ?name
       . [:window _ (and !child1 !child2) _ _] ...])
   (r.subst/substitute
    {:name ?name
     :componentType "StackedWindow"
     :customData {:spawnData {:windowIdentifier
                              [{:windowName !child2} ...]}}
     :childWindowIdentifiers [{:windowName !child1} ...]})))

(defn transform [expr]
  {:name "foo"
   :type "workspace"
   :version "1.0.0"
   :windows (flatten ((juxt normalWindows stackedWindows) expr))
   :groups {}
   :componentState (componentState expr)})



(transform (parse ex1))
