(ns fsbl-tester.parse
  #:ghostwheel.core{:check true
                    :num-tests 10}
  (:require [fsbl-tester.workspace :as ws]
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
(>defn stringify-map
       [my-map]
       [map? => map? | #(every? string? (keys %))]
       (reduce (fn [m k v]
                 (.log js/console m k v)
                 (assoc m (name (str k)) v))
                  my-map {}))

(stringify-map {0 1})
(-> 0 str name)
(g/check)
(def ex1  "
Welcome Component FIRST with bounds 100 200 300 400
Stacked Window STACK1 of
  Welcome Component B on linker channel purple
  Welcome Component FOOBAR on linker channel yellow
")

(def color-map {"yellow" :group1
                "purple" :group2})
(defn parse [s]
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
(componentState (parse ex1))
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
(defn stackedWindows [expr]
 (r.match/search
           example
           ($ [:stack
               ?name
               . [:window _ (and !child1 !child2) _ _] ...])
           (r.subst/substitute
            {:name ?name
             :componentType "StackedWindow"
             :customData {:spawnData {:windowIdentifier
                                      [{:windowName !child2} ...]}}
             :childWindowIdentifiers [{:windowName !child1} ...]})))

(defn windows
  [expr]
  (concat (normalWindows expr)
          ))

(defn transform [expr]
  {:name "foo"
   :type "workspace"
   :version "1.0.0"
   :windows (concat (juxt )expr)
   :groups {}
   :componentState (componentState expr)})

((juxt normalWindows stackedWindows) expr)
((comp concat ))

(transform example)
