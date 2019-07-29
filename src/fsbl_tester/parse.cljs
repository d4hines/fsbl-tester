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

((r/rewrite
  {!parent [!children ...]}
  [. !parent . !children ... ...])
 {:foo [:child1 :child2]
  :bar [:child3 :child4]})

((r/rewrite
  {!parent [!children ...]}
  [!parent . !children ...])
 {:foo [:child1 :child2]
  :bar [:child3 :child4]})

(def color->group {"yellow" :group1
                   "purple" :group2})

(def group->color {:group1 "yellow"
                   :group2 "purple"})

(defn channels->componentState [& args]
  {:Finsemble_Linker
   (reduce #(assoc %1 %2 true) {} args)})

(def replace_ #(if (nil? %) "_" %))
(def underscore? (partial = "_"))

(>defn parse
       [s]
       [string? => ::ws/workspace-ast]
       (let [cleanStr #(str/replace % #",|and|\:" "")
             parser (insta/parser "
S = <'Given'?> window*

name = #'[a-zA-Z0-9]+'
number = #'[0-9]+' | '_'

window = component name bounds linkerChannel
component = 'Welcome Component' | 'Notepad'
bounds = (<'with bounds'> number number number number) | ''
linkerChannel = (<'on linker channel'| 'on linker channels'> channel+) | ''

channel = 'yellow' | 'purple'
" :auto-whitespace :standard :string-ci true)]
         (->> s cleanStr parser
              (insta/transform {:number #(if (= % "_") nil (int %))
                                :name identity
                                :channel color->group
                                :linkerChannel channels->componentState
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

(defn windows [expr]
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


(defn unparse
  [ast]
  (let [unparse
        (r/rewrite
         [:S . [:window
                !component
                !name
                ((app replace_ !top)
                 (app replace_ !left)
                 (app replace_ !width)
                 (app replace_ !height))
                _] ...]
         [!component " " !name
          " with bounds " !top " " !left " " !width " " !height
          ",\n" ...])]
    (->> (unparse ast) (drop-last 1) str/join)))

(parse (unparse (parse "Welcome Component A with bounds 0 0 100 100")))
(unparse (ws/gen ::ws/workspace-ast))
(defn transform [expr name]
  {:name name
   :type "workspace"
   :version "1.0.0"
   :windows (windows expr)
   :groups {}
   :componentState (componentState expr)})






















