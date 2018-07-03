(ns musicmath.core
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch dispatch-sync]]
              ; [devtools.core :as devtools]
            [material-ui]
            [musicmath.views]
            [musicmath.events]
            [musicmath.subs]
            [musicmath.defs]
            [stylefy.core :as stylefy]))

; (devtools/install!)
; (enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defn ^:export main
  []
  (stylefy/init {:global-vendor-prefixes {::stylefy/vendors ["webkit" "moz" "o"]
                                        ::stylefy/auto-prefix #{:border-radius :box-shadow :appearance}}})
  (dispatch-sync [:initialize-db])
  (reagent/render [musicmath.views/app]
                  (.getElementById js/document "app")))

(defn on-js-reload
  []
  ; (js/console.log js/MaterialUI)
  (stylefy/init {:global-vendor-prefixes {::stylefy/vendors ["webkit" "moz" "o"]
                                        ::stylefy/auto-prefix #{:border-radius :box-shadow :appearance}}})
  (dispatch-sync [:initialize-db])
  (main))