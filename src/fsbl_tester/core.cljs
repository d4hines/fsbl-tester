;;;;;;;;;;;;;;;;;; Project Setup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(ns fsbl-tester.core
  #:ghostwheel.core{:check true
                    :num-tests 10}
  (:require [reagent.core :as reagent]
            [fsbl-tester.view :refer [view]]
            [fsbl-tester.workspace :as ws]
            [fsbl-tester.api :as api]
            [fsbl-tester.parse :as parse]

            [clojure.spec.alpha :as s :refer [spec]]
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
