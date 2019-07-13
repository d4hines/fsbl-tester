;;;;;;;;;;;;;;;;;; Project Setup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(ns fsbl-tester.core
  #:ghostwheel.core{:check true
                    :num-tests 10}
  (:require [reagent.core :as reagent]
            [fsbl-tester.view :refer [view]]
            [fsbl-tester.workspace :as ws]
            [fsbl-tester.api :as api]

            [clojure.string :as str]
            [clojure.spec.alpha :as s :refer [spec]]
            [clojure.spec.gen.alpha :as gen :refer [generate]]
            [instaparse.core :as insta]
            [meander.match.delta :as r.match :include-macros true]
            [meander.substitute.delta :as r.subst]
            [meander.strategy.delta :as r :include-macros true]

            [clojure.test.check.generators]
            [expound.alpha]
            [ghostwheel.core :as g
             :refer [>defn >defn- >fdef => | <- ?]])

  (:require-macros [cljs.core.async.macros :refer [go]]))

(set! s/*explain-out* expound.alpha/printer)

;;;;;;;;;;;;;;;;;;;;;;; Hot Reloading ;;;;;;;;;;;;;;;;;;;;;
(defn start []
  (g/check #"fsbl-tester.*")
  (reagent/render-component [view]
                            (. js/document (getElementById "app"))))
(defn ^:export init []
  (start))

(defn stop []
  (js/console.log "Hot reloading fsbl-tester.core..."))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
