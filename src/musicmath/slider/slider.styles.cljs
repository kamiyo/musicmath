(ns musicmath.slider.styles
  (:require [stylefy.core :as stylefy :refer [use-style]]))

(defn container-style [dragging?]
  {:cursor (if dragging? "pointer" "unset")
   :display "flex"
   :align-items "center"
   :justify-content "center"
   :padding "24px"})

(defn slider-style [width]
  {:display "inline-block"
   :position "relative"
   :width (str width "px")
   :height "50px"
   :vertical-align "middle"
   :margin "16px 16px 8px 16px"})

(defn thumb-style [thumb-left dragging? multiplier color]
  {:position "absolute"
   :width "11px"
   :height "11px"
   :border (str "2px solid " color)
   :border-radius "50%"
   :left (str thumb-left "px")
   :top "50%"
   :margin "0 1px"
   :transform (str "translate3d(-50%, -50%, 0)" (when dragging? " scale(1.2)"))
   :background-color (if (= 0 multiplier) "white" color)
   :z-index 1
   :text-align "center"
   :transition "transform 0.2s, left 0.02s ease-in-out"
   ::stylefy/mode {:hover
                   {:cursor "pointer"
                    :transform "translate3d(-50%, -50%, 0) scale(1.2)"}}})

(defn track-before-style [thumb-left color]
  {:position "absolute"
   :width (str thumb-left "px")
   :height "2px"
   :top "50%"
   :background-color color
   :border-radius "2px 0 0 2px"
   :transform "translateY(-50%)"
   :transition "width 0.02s ease-in-out"
   :z-index 0})

(defn track-after-style [thumb-left width dragging?]
  {:position "absolute"
   :width (str (- width thumb-left) "px")
   :left (str thumb-left "px")
   :height "2px"
   :top "50%"
   :background-color (if dragging? "#ccc" "#eee")
   :border-radius "0 2px 2px 0"
   :transition "background-color 0.1s, width 0.02s ease-in-out, left 0.02s ease-in-out"
   :transform "translateY(-50%)"
   :z-index 0})
