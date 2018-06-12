(ns musicmath.views.slider
  (:require [goog.events :as events]
            [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [stylefy.core :as stylefy :refer [use-style]]
            [musicmath.defs :refer [my-blue]])
  (:import [goog.events EventType]))

(defn drag-move-fn [on-drag]
  (fn [evt]
    (let [target (.-target evt)]
      (on-drag {:offsetX (.-clientX evt)}))))

(defn my-drag-move [{:keys [min max width event]}]
  (let [dist (- max min)
        offset (/ width dist)
        slider-left (-> event .-nativeEvent .-target .-parentElement .-offsetLeft)]
    (fn [{:keys [offsetX]}]
      (let [x (- offsetX slider-left)
            tick (/ x offset)]
        (dispatch [:update-multiplier (js/Math.round tick)])))))

(defn drag-end-fn [drag-move drag-end on-end]
  (fn [evt]
    (events/unlisten js/window EventType.MOUSEMOVE drag-move)
    (events/unlisten js/window EventType.MOUSEUP @drag-end)
    (on-end)))

(defn drag-start
  ([on-drag] (drag-start on-drag (fn []) (fn [])))
  ([on-drag on-start] (drag-start on-drag on-start (fn [])))
  ([on-drag on-start on-end]
   (let [drag-move (drag-move-fn on-drag)
         drag-end-atom (atom nil)
         drag-end (drag-end-fn drag-move drag-end-atom on-end)]
     (on-start)
     (reset! drag-end-atom drag-end)
     (events/listen js/window EventType.MOUSEMOVE drag-move)
     (events/listen js/window EventType.MOUSEUP drag-end))))

(defn container-style [dragging?]
  {:cursor (if dragging? "pointer" "unset")})

(defn slider-style [width]
  {:display "inline-block"
   :position "relative"
   :width (str width "px")
   :height "20px"
   :vertical-align "middle"
   :margin "0 10px"})

(defn thumb-style [thumb-left dragging? multiplier]
  {:position "absolute"
   :width "11px"
   :height "11px"
   :border (str "2px solid " (:hex my-blue))
   :border-radius "50%"
   :left (str thumb-left "px")
   :top "50%"
   :margin "0 1px"
   :transform (str "translate3d(-50%, -50%, 0)" (when dragging? " scale(1.2)"))
   :background-color (if (= 0 multiplier) "white" (:hex my-blue))
   :z-index 1
   :text-align "center"
   :transition "transform 0.2s, left 0.02s ease-in-out"
   ::stylefy/mode {:hover
                   {:cursor "pointer"
                    :transform "translate3d(-50%, -50%, 0) scale(1.2)"}}})

(defn track-before-style [thumb-left]
  {:position "absolute"
   :width (str thumb-left "px")
   :height "2px"
   :top "50%"
   :background-color (:hex my-blue)
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

(defn multiplier-slider
  [idx]
  (let [multiplier @(subscribe [:multiplier idx])
        input      @(subscribe [:input idx])
        dragging?  @(subscribe [:dragging? idx])
        width      300
        thumb-left (* 300 (/ multiplier 20))]
    [:div.multiplier-container
     (use-style (container-style dragging?))
     [:div.multiplier-slider
      (use-style
       (slider-style width)
       {:on-wheel
        #(let
          [dy (-> % .-deltaY)
           d-fn (cond (pos? dy) dec
                      (neg? dy) inc)]
           (dispatch [:update-multiplier (d-fn multiplier)]))})
      [:div.thumb
       (use-style
        (thumb-style thumb-left dragging? multiplier)
        {:on-mouse-down
         (fn [evt]
           (drag-start
            (my-drag-move {:min 0
                           :max 20
                           :width width
                           :event evt})
            #(dispatch [:update-dragging true])
            #(dispatch [:update-dragging false])))})]
      [:div.track-before (use-style (track-before-style thumb-left))]
      [:div.track-after (use-style (track-after-style thumb-left width dragging?))]]
     [js/MaterialUI.TextField
      (clj->js {:label "multiplier"
                :name "multiplier"
                :fullWidth false
                :error (js/isNaN (js/parseInt input))
                :type "number"
                :min "0"
                :max "20"
                :step "1"
                :value input
                :inputProps {:style {:height "20px"
                                     :width "3rem"
                                     :display "inline-block"}}
                :onChange #(dispatch [:update-input (-> % .-target .-value)])
                :onBlur #(when (= "" (-> % .-target .-value))
                           (dispatch [:update-multiplier multiplier]))})]]))