(ns musicmath.slider
  (:require [goog.events :as events]
            [goog.object :refer [getValueByKeys]]
            [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [stylefy.core :as stylefy :refer [use-style]]
            [musicmath.slider.styles :refer [thumb-style
                                             container-style
                                             slider-style
                                             track-before-style
                                             track-after-style]])
  (:import [goog.events EventType]))

(defn dispatch-update-value
  [tone-id node-id value]
  (dispatch [:update-value tone-id node-id value]))

(defn dispatch-update-input
  [tone-id node-id value]
  (dispatch [:update-input tone-id node-id value]))

(defn log-position-to-value [x min-log scale]
  (js/Math.pow 2 (+ (* scale x) min-log)))

(defn value-to-log-position [val min-log scale]
  (/ (- (js/Math.log2 val) min-log) scale))

(defn drag-move-fn [on-drag]
  (fn [evt]
    (let [target (.-target evt)]
      (on-drag {:offsetX (.-clientX evt)}))))

(defn my-drag-move [{:keys [update-value min max width event]}]
  (let [dist (- max min)
        offset (/ width dist)
        slider-left (-> event .-nativeEvent .-target .-parentElement .-offsetLeft)]
    (fn [{:keys [offsetX]}]
      (let [x (- offsetX slider-left)
            tick (/ x offset)]
        (update-value (js/Math.round tick))))))

(defn my-drag-move-log [{:keys [update-value min-log scale event]}]
  (let [slider-left (-> event .-nativeEvent .-target .-parentElement .-offsetLeft)]
    (fn [{:keys [offsetX]}]
      (let [x (- offsetX slider-left)
            value (log-position-to-value x min-log scale)]
        (update-value value)))))

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

(defn thumb
  [{:keys [thumb-left
           value
           slider-props
           color
           update-value
           min-log
           scale]}]
  [:div.thumb
   (use-style
    (thumb-style thumb-left (:dragging? slider-props) value color)
    {:on-mouse-down
     (fn [evt]
       (drag-start
        (my-drag-move-log {:update-value update-value
                           :min-log      min-log
                           :scale        scale
                           :event        evt})
        #(dispatch [:update-dragging true])
        #(dispatch [:update-dragging false])))})])

(defn slider
  [props & children]
  (let [{:keys [thumb-left width min-log scale update-value]} props
        scroll-amount (* .01 width)]
    [:div.slider
     (use-style
      (slider-style width)
      {:on-wheel
       (fn [evt]
         (let
          [dy   (.-deltaY evt)
           d-fn (cond (pos? dy) #(- % scroll-amount)
                      (neg? dy) #(+ % scroll-amount))]
           (update-value (log-position-to-value (d-fn thumb-left) min-log scale))))})
     children]))

(defn num-display-style
  [theme]
  (let [spacing-unit (-> theme .-spacing .-unit)]
    (clj->js
     {:textField {:marginLeft spacing-unit
                  :marginRight spacing-unit}})))

(defn num-display
  [props]
  (let [{:keys [type value input sliderProps updateInput classes]} (js->clj props :keywordize-keys true)
        float-input (js/parseFloat input)
        n-min (js/parseFloat (:min sliderProps))
        n-max (js/parseFloat (:max sliderProps))
        error? (or (js/isNaN float-input) (> n-min float-input) (< n-max float-input))]
    [js/MaterialUI.TextField
     (clj->js {:label (if error? "error" (name type))
               :name (name type)
               :className (:textField classes)
               :fullWidth false
               :type "number"
               :min (:min sliderProps)
               :max (:max sliderProps)
               :helperText (if error? "15 < n < 8000" " ")
               :step "1"
               :error error?
               :margin "normal"
               :value input
               :InputLabelProps {:style {:userSelect "none"}}
               :inputProps      {:style {:height     "20px"
                                         :width      "5rem"
                                         :display    "inline-block"}}
               :onChange #(updateInput (-> % .-target .-value))
               :onBlur #(when (= "" (-> % .-target .-value))
                          (updateInput (.toPrecision (js/Number. (-> % .-target .-value)) 6)))})]))

(def num-display-with-style
  (let [decorator (js/MaterialUIStyles.withStyles num-display-style)]
    (reagent/adapt-react-class (decorator (reagent/reactify-component num-display)))))

(defn slider-group
  [props]
  (let [{:keys [tone-id node-id node theme]} props
        clj-node     (js->clj node :keywordize-keys true)
        type         (:type clj-node)
        value        ((keyword type) clj-node)
        slider-props (:slider clj-node)
        input        (:input slider-props)
        width        300
        max-log      (js/Math.log2 (js/parseFloat (:max slider-props)))
        min-log      (js/Math.log2 (js/parseFloat (:min slider-props)))
        scale        (/ (- max-log min-log) width)
        thumb-left   (value-to-log-position value min-log scale)
        primary-dark (-> theme .-palette .-primary .-dark)
        update-value (partial dispatch-update-value tone-id node-id)
        update-input (partial dispatch-update-input tone-id node-id)
        common-props {:value value
                      :thumb-left thumb-left
                      :slider-props slider-props
                      :color primary-dark
                      :update-value update-value
                      :scale scale
                      :min-log min-log}]
    [:div.slider-container
     (use-style (container-style (:dragging? slider-props)))
     [slider (merge common-props {:width width})
      [thumb (merge common-props {:key "thumb"})]
      [:div.track-before (use-style (track-before-style thumb-left primary-dark) {:key "track-before"})]
      [:div.track-after (use-style (track-after-style thumb-left width (:dragging? slider-props)) {:key "track-after"})]]
     [num-display-with-style {:type         type
                              :value        value
                              :slider-props slider-props
                              :input        input
                              :update-input update-input}]]))

(defn slider-group-with-theme [tone-id node-id node]
  (let [sg (reagent/adapt-react-class
            ((js/MaterialUI.withTheme)
             (reagent/reactify-component slider-group)))]
    (fn [tone-id node-id node]
      [sg {:toneid tone-id :node-id node-id :node node}])))

(defn multiplier-slider
  [])