(ns musicmath.views
  (:require [goog.events :as events]
            [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [stylefy.core :as stylefy :refer [use-style]]
            [musicmath.views.slider :refer [multiplier-slider]]
            [musicmath.defs :refer [my-blue]])
  (:import [goog.events EventType]))

(def theme
  (js/MaterialUIStyles.createMuiTheme
   (clj->js
    {:palette
     {:primary
      {:main (:hex my-blue)}}})))

(defn styles
  [theme]
  (let [gutter-fn (-> theme .-mixins .-gutters)
        spacing-unit (-> theme .-spacing .-unit)]
    (clj->js
     {:root (gutter-fn (clj->js
                 {:padding 16
                  :margin (* 3 spacing-unit)}))})))

(defn paper-with-style
  [children]
  (let [decorator (js/MaterialUIStyles.withStyles styles)]
    [(reagent/adapt-react-class
      (decorator js/MaterialUI.Paper))
     children]))

(defn app
  []
  (let [my-theme theme
        number-of-tones @(subscribe [:number-of-tones])]
    [(reagent/adapt-react-class js/MaterialUIStyles.MuiThemeProvider) {:theme my-theme}
     [paper-with-style
      [(reagent/adapt-react-class js/MaterialUI.CssBaseline)
      (for [idx (range number-of-tones)] ^{:key idx} [multiplier-slider idx])]]]))