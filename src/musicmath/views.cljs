(ns musicmath.views
  (:require [goog.events :as events]
            [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [stylefy.core :as stylefy :refer [use-style]]
            [musicmath.slider :refer [slider-group-with-theme]])
  (:import [goog.events EventType]))

(def theme
  (js/MaterialUI.createMuiTheme
   (clj->js
    {:palette
     {:primary
      {:main (:200 (js->clj js/MaterialUI.colors.indigo :keywordize-keys true))}
      :secondary
      {:main (:200 (js->clj js/MaterialUI.colors.red :keywordize-keys true))}}
     :overrides
     {:MuiAppBar
      {:root
       {:userSelect "none"}}}})))

(defn styles
  [theme]
  (let [spacing-unit (-> theme .-spacing .-unit)]
    (clj->js
     {:root {:margin (* 3 spacing-unit)}})))

(defn tone-container
  [tone-id tone]
  (let [decorator (js/MaterialUI.withStyles styles)
        paper-with-style (decorator js/MaterialUI.Paper)]
    (fn [tone-id tone]
      [(reagent/adapt-react-class paper-with-style) {:elevation 4}
       [:div
        [(reagent/adapt-react-class js/MaterialUI.AppBar)
         {:position "static"
          :color "primary"
          :elevation 0}
         [(reagent/adapt-react-class js/MaterialUI.Toolbar)
          [(reagent/adapt-react-class js/MaterialUI.Typography)
           {:variant "title"}
           (str "Tone " (inc tone-id))]]]
        (let [nodes @(subscribe [:get-nodes tone-id])]
          (map-indexed
           (fn [node-id node]
             ^{:key (str "node-" tone-id "-" node-id)} [slider-group-with-theme tone-id node-id node])
           nodes))]])))

(defn app
  []
  (let [my-theme theme
        tones @(subscribe [:get-tones])]
    [:div {:style {:width "100%" :max-width "1000px" :margin "auto"}}
     [(reagent/adapt-react-class js/MaterialUI.CssBaseline)]
     [(reagent/adapt-react-class js/MaterialUI.MuiThemeProvider) {:theme my-theme}
      (map-indexed
       (fn [tone-id tone]
         ^{:key (str "tone-" tone-id)} [tone-container tone-id tone])
       tones)]]))