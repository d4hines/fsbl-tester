(ns fsbl-tester.spec
  (:require [clojure.spec.alpha :as s :refer [spec]]
            [clojure.spec.gen.alpha :as gen :refer [generate]]
            [instaparse.core :as insta]
            [clojure.test.check.generators]
            [cljs.pprint :refer [pprint]]
            [meander.match.delta :as r.match :include-macros true]
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

(defn gen [p] (gen/generate (s/gen p)))

(defn parse [s]
  (let [cleanStr #(clojure.string/replace %
                                          #",|and|\:"
                                          "")
        parser (insta/parser "
S = <'Given'?> (window | group)+

name = #'[a-zA-Z0-9]+'
number = #'[0-9]+'

window = component name feature*
component = 'Welcome Component' | 'Notepad'
feature = bounds | linkerChannel
bounds = <'with bounds'> number number (number number)?
linkerChannel = <'on linker channel'> channel
channel = 'yellow' | 'purple' | 'green' | 'red' 

stack = ('Stacked Window' | 'Stack') name ('with children' | 'of') window window+ <';'?>
group = <'Group' | 'a group'> <name> <'containing'> (window | stack) (window | stack)+ ';'?
" :auto-whitespace :standard :string-ci true)]
    (->> s cleanStr parser (insta/transform {:number int}))))

(def win (parse "
A Group G1 containing Welcome Component A with bounds 100 200 300 400
and Welcome Component B;
Welcome Component FOOBAR
and Group G1 containing Notepad C and Notepad D"))
(random-uuid)
(parse "
Welcome Component A
on linker channel yellow
with bounds 100 200 300 400
")


;; There are 3 "kinds" of things:
;; - Windows
;; - Stacked Windows, which have 2 or more normal Windows as children
;; - Groups, which of collections comprised of normal Windows and/or Stacked Windows

;; We have an old and crusty API format that's got tons of duplication, etc.
;; The task is to parse a controlled natural language into this API format.


(apply merge (r.match/search
              [[:foo :bar]
               [:group [:name "foo"]
                [:window [:name "w1"]]
                [:window [:component] [:name "w2"]]]
               [:group [:name "bar"]
                [:window [:name "w3"] :foo]
                [:window [:name "w4"]]]]
              [_ ... [:group
                      [:name ?group-name]
                      . [:window . _ ...
                         [:name !window-name]
                         . _ ...] ..2]
               . _ ...]
              {?group-name {:windowNames !window-name
                            :isMovable false}
               (random-uuid) {:windowNames !window-name
                              :isMovable true}}))

{:windows (r.match/search
             [:group
              [:window [:component "WC"] [:name "foo"]
               [:feature [:bounds 0 100 200 300]]]
              [:window [:component "WC"] [:name "bar"]]]
             (scan [:window
                    [:component ?component]
                    [:name ?name]
                    . _ ...
                    ;; Ideally, this whole feature should be optional
                    ;; and every bounds should also default to js/undefined
                    [:feature [:bounds ?top ?left ?width ?height]]
                    . _ ...])
             {:componentType ?component
              :name  ?name
              :defaultTop ?top
              :defaultLeft ?left
              :defaultWidth ?width
              :defaultHeight ?height})}

(r.match/search
 [[:window [:name "w1"] :foobar]
  [:window [:component] [:name "w2"]]]
 [_ ... [:window
         (scan [:name ?window-name])]

  . _ ...]
 ?window-name)

{:groups {& [[(random-uuid) {:windowNames [!window-name ...]
                             :isMovable true}] ...]}}
((r/rewrite
  [:S [:group [:name ?group-name]
       . [:window [:name !window-name]
          [:feature
           [:bounds !top !left !height !width]
           [:linkerChannel !linker-channel]]]
       ...]]
  {:windows [{:name !window-name
              :defaultTop !top
              :defaultLeft !left
              :defaultHeight !height
              :defaultWidth !width} ...]})
 [:S [:group
      [:name "G1"]
      [:window
       [:name "W1"]
       [:feature
        [:bounds 100 200 300 400]
        [:linkerChannel "yellow"]]]
      [:window
       [:name "W2"]
       [:feature
        [:bounds 100 200 300 400]
        [:linkerChannel "yellow"]]]]])

((r/rewrite
  [:S [:group [:name ?group-name]
       . [:window [:name !window-name]
          [:feature
           [:bounds !top !left !height !width]
           [:linkerChannel !linker-channel]]]
       ...]]
  {:windows [{:name !window-name
              :defaultTop !top
              :defaultLeft !left
              :defaultHeight !height
              :defaultWidth !width} ...]
   :componentState {& [[!window-name
                        {:Finsemble_Linker
                         {& [[!linker-channel true] ...]}}]
                       ...]}})
 [:S [:group
      [:name "G1"]
      [:window
       [:name "W1"]
       [:feature
        [:bounds 100 200 300 400]
        [:linkerChannel "yellow"]]]
      [:window
       [:name "W2"]
       [:feature
        [:bounds 100 200 300 400]
        [:linkerChannel "yellow"]]]]])
(comment

  ((r/rewrite
    [:S [:group [:name ?group-name]
         . [:window [:name !window-name]
            [:feature
             [:bounds !top !left !height !width]
             [:linkerChannel !linker-channel]]]
         ...]]

    {:groups #{?group-name {:windowNames [!window-name ...]}
               :isMovable true}

     :windows [{:name !window-name
                :defaultTop !top
                :defaultLeft !left
                :defaultHeight !height
                :defaultWidth !width} ...]
     :componentState {& [[!window-name
                          {:Finsemble_Linker
                           {& [[!linker-channel true] ...]}}]
                         ...]}})
   [:S [:group
        [:name "G1"]
        [:window
         [:name "W1"]
         [:feature
          [:bounds 100 200 300 400]
          [:linkerChannel "yellow"]]]
        [:window
         [:name "W2"]
         [:feature
          [:bounds 100 200 300 400]
          [:linkerChannel "yellow"]]]]])

  (-> (parse "
Given:
Welcome Component A with bounds 0, 100, 200, 300, and on linker channel yellow,
Notepad B with bounds 100 and 200,
Group G1 containing Welcome Component C and Notepad D with bounds 100, 200, 300, 444
and Stacked Window S1 with children Welcome Component WCS1 and Welcome Component WCS2")
      (nth 3))

  (r/search
   [:S [:group
        [:name "G1"]
        [:window
         [:name "W1"]
         [:feature
          [:bounds 100 200 300 400]
          [:linkerChannel "yellow"]]]]]
   [:S
    [:group [:name ?group-name]
     & (r.match/scan
        [:window [:name ?window-name]
         & (r.match/scan [:feature & (r.match/scan [:bounds ?top ?left ?height ?width])])])]]
   [?group-name ?window-name [:bounds ?top ?left ?height ?width]])

  ((r/rewrite
    [:S [:group [:name ?group-name]
         . [:window [:name !window-name]
            (or [:feature
                 [:bounds !top !left !height !width]
                 [:linkerChannel !linker-channel]]

                (and [:feature
                      [:bounds !top !left !height !width]]
                     (m/let !linker-channel "default-channel"))

                (and [:feature
                      [:linkerChannel !linker-channel]]
                     (m/let [!top !left !height !width] [0 0 100 100]))

                (let [!top !left !height !width] [0 0 100 100]))]

         ...]]

    {:groups {?group-name ~(set !window-name)}
     :windows {& [[!window-name {:defaultTop !top
                                 :defaultLeft !left
                                 :defaultHeight !height
                                 :defaultWidth !width}] ...]}})
   [:S [:group
        [:name "G1"]
        [:window
         [:name "W1"]
         [:feature
          [:bounds 100 200 300 400]
          [:linkerChannel "yellow"]]]
        [:window
         [:name "W2"]
         [:feature
          [:bounds 100 200 300 400]]]
        [:window
         [:name "W3"]
         [:feature
          [:linkerChannel "yellow"]]]
        [:window
         [:name "W4"]
         [:feature
          [:linkerChannel "yellow"]]]]]))

