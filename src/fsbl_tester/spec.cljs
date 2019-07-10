(ns fsbl-tester.spec
  (:require [clojure.spec.alpha :as s :refer [spec]]
            [clojure.spec.gen.alpha :as gen :refer [generate]]
            [instaparse.core :as insta]
            [clojure.test.check.generators]
            [cljs.pprint :refer [pprint]]
            [meander.match.delta :as r.match :include-macros true]
            [meander.substitute.delta :as r.subst]
            [meander.strategy.delta :as r :include-macros true]
            [expound.alpha]))
(set! s/*explain-out* expound.alpha/printer)
(defn kw->string [my-map path]
  (update-in my-map path
             #(reduce-kv
               (fn [m k v]
                 (assoc m (name k) v))
               {} %)))

(def ws-clj (-> ws
                (kw->string [:windows])
                (kw->string [:groups])))

(defn color->group
  [color]
  (get {"yellow" :group1
        "purple" :group2} color))

(do
  (s/def ::name (s/and string? (partial not= "")))
  (s/def ::windowNames (s/coll-of ::name))
  (s/def ::isMovable boolean?)
  (s/def ::group (s/keys :req-un [::isMovable ::windowNames]))
  (s/def ::groups (s/map-of ::name ::group))
  (s/def ::linkerGroup #{:group1 :group2 :group3 :group4 :group5 :group6})
  (s/def ::Finsemble_Linker (s/map-of ::linkerGroup boolean?))
  (s/def ::componentState (s/keys :opt-un [::Finsemble_Linker]))

  (s/def ::componentType #{"Welcome Component" "WPFExample" "Notepad"})

  (s/def ::bound (s/int-in 0 2000))
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
  (s/def ::windows (s/coll-of ::window))
  (s/def ::version #{"1.0.0"})
  (s/def ::type #{"workspace"})
  (s/def :workspace/name string?)

  (s/def ::workspace (s/keys :req-un [:workspace/name
                                      ::version
                                      ::type
                                      ::groups
                                      ::windows])))

(defn gen [p] (gen/generate (s/gen p)))

(defn parse [s]
  (let [cleanStr #(clojure.string/replace %
                                          #",|and|\:"
                                          "")
        parser (insta/parser "
S = <'Given'?> (window | group | stack)+

name = #'[a-zA-Z0-9]+'
number = #'[0-9]+'

window = component name feature*
component = 'Welcome Component' | 'Notepad'
feature = bounds | linkerChannel
bounds = <'with bounds'> number number (number number)?
linkerChannel = <'on linker channel'> channel
channel = 'yellow' | 'purple' | 'green' | 'red' 

stack = <('Stacked Window' | 'Stack')> name <('with children' | 'of')> window window+ <';'?>
group = <'Group' | 'a group'> name <'containing'> (window | stack) (window | stack)+ <';'>?
" :auto-whitespace :standard :string-ci true)]
    (->> s cleanStr parser (insta/transform {:number int
                                             :channel color->group}))))

(def example (parse "
A Group G1 containing Welcome Component A with bounds 100 200 300 400
and Welcome Component B on linker channel purple;
Welcome Component FOOBAR on linker channel yellow
and Group G2 containing Notepad C and Notepad D"))




;; There are 3 "kinds" of things:
;; - Windows
;; - Stacked Windows, which have 2 or more normal Windows as children
;; - Groups, which of collections comprised of normal Windows and/or Stacked Windows

;; We have an old and crusty API format that's got tons of duplication, etc.
;; The task is to parse a controlled natural language into this API format.
(defn groups [expr]
  (into {}
        (r.match/search
         expr
         (scan [:group
                [:name ?group-name]
                . (or [:window . _ ...
                       [:name !window-name]
                       . _ ...]
                      [:stack . _ ...
                       [:name !window-name]
                       . _ ...]) ..2])
         {?group-name {:windowNames !window-name
                       :isMovable false}
          (str (random-uuid)) {:windowNames !window-name
                               :isMovable true}})))

(defn componentState [expr]
  (r.match/search
   expr
   ($ [:window
       _
       [:name !name]
       . _ ...
       [:feature [:linkerChannel !channel]]
       . _ ...])
   (r.subst/substitute
    {& [[!name {:Finsemble_Linker
                {!channel true}}] ...]})))

;; Windows can either be stacks or normal windows, and both
;; need to go in the same "bucket".
(defn windows 
  [expr]
  (concat (r.match/search
           expr
           ($ (scan [:window
                     [:component ?component]
                     [:name ?name]
                     . _ ...
                     ;; Ideally, this whole feature should be optional
                     ;; and every bounds should also default to js/undefined
                     [:feature [:bounds ?top ?left ?width ?height]]
                     . _ ...]))
           {:componentType ?component
            :name ?name
            :defaultTop ?top
            :defaultLeft ?left
            :defaultWidth ?width
            :defaultHeight ?height})

          (r.match/search mix
                          ($ (scan [:stack
                                    [:name ?name]
                                    . [:window _ [:name !child] . _ ...] ...]))
                          (r.subst/substitute
                           {:name ?name
                            :componentType "StackedWindow"
                            :childWindowIdentifiers [{:windowName !child} ...]}))))
(defn transform [expr]
  {:name "foo"
   :type "workspace"
   :version "1.0.0"
   :windows (windows expr)
   :groups (groups expr)
   :componentState (componentState expr)})

(s/valid? ::workspace (transform example))