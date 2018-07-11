(ns musicmath.slider
  (:require [goog.events :as events]
            [reagent.core :as r]
            [clojure.string :refer [capitalize]]
            [re-frame.core :refer [subscribe dispatch]]
            [stylefy.core :as stylefy :refer [use-style]]
            [musicmath.slider.styles :refer [thumb-style
                                             container-style
                                             slider-style
                                             track-before-style
                                             track-after-style]]
            [musicmath.defs :refer [closest-note
                                    closest-note-name
                                    closest-note-octave
                                    cents-from-closest-note
                                    freq-from-note
                                    freq-from-note-name
                                    half-step-down
                                    half-step-up
                                    note-map]])
  (:import [goog.events EventType]))

(defn dispatch-update-value
  [tone-id node-id value]
  (dispatch [:update-value tone-id node-id value]))

(defn log-position-to-value [x min-log scale]
  (js/Math.pow 2 (-> x (* scale) (+ min-log))))

(defn value-to-log-position [val min-log scale]
  (-> (js/Math.log2 val) (- min-log) (/ scale)))

(defn drag-move-fn [on-drag]
  (fn [evt]
    (on-drag {:offsetX (.-clientX evt)})))

(defn my-drag-move [{:keys [update-value min max width event]}]
  (let [offset (->> min (- max) (/ width))
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
  (let [{:keys [thumb-left width min-log scale update-value value]} props
        scroll-amount (* .01 width)]
    [:div.slider
     (use-style
      (slider-style width)
      {:on-wheel
       (fn [evt]
         (let
          [dy        (.-deltaY evt)
           new-value (cond (pos? dy) (half-step-down value)
                           (neg? dy) (half-step-up value))]
           (update-value new-value)))})
     children]))

(defn display-style
  [theme]
  (let [spacing-unit (-> theme .-spacing .-unit)]
    (clj->js
     {:textField {:marginLeft  spacing-unit
                  :marginRight spacing-unit}})))

(defn num-display
  [props]
  (let [state   (atom   {:edited? false})
        r-state (r/atom {:temp    nil
                         :error?  false})]
    (fn [props]
      (let [{:keys [type value updateValue]
             {:keys [textField]} :classes
             {:keys [min max]}   :sliderProps} (js->clj props :keywordize-keys true)
            name                               (name type)]
        [(r/adapt-react-class js/MaterialUI.TextField)
         {:label      name
          :name       name
          :className  textField
          :fullWidth  false
          :type       "number"
          :min        min
          :max        max
          :helperText (if (:error? @r-state) "error" " ")
          :step       "1"
          :error      (:error? @r-state)
          :margin     "normal"
          :value      (or (:temp @r-state) (-> value js/Number. (.toFixed 3)))
          :InputProps      {:endAdornment (r/as-element
                                           [(r/adapt-react-class js/MaterialUI.InputAdornment) {:position "end"} "hz"])}
          :InputLabelProps {:style {:userSelect "none"}}
          :inputProps      {:style {:height     "20px"
                                    :width      "6rem"
                                    :display    "inline-block"}}
          :onChange   (fn [ev]
                        (let [new-value (-> ev .-target .-value)]
                          (if (true? (:edited? @state))
                            ((fn []
                               (swap! r-state assoc :temp new-value :error? (js/isNaN (js/parseFloat new-value)))
                               (swap! state assoc :edited? false)))
                            ((fn []
                               (swap! r-state assoc :temp nil :error? false)
                               (updateValue new-value))))))
          :onKeyDown  (fn [ev]
                        (let [key       (.-key ev)
                              new-value (-> ev .-target .-value)
                              set-edit  (swap! state assoc :edited? true)]
                          (condp re-matches key
                            #"Enter" :>> (fn [_]
                                           (swap! r-state assoc :temp nil)
                                           (updateValue new-value))
                            #"([0-9.]*)" :>> #(set-edit)
                            #"Backspace" :>> #(set-edit)
                            #"Delete"    :>> #(set-edit)
                            ())))
          :onBlur     (fn [ev]
                        (swap! r-state assoc :temp nil)
                        (updateValue (-> ev .-target .-value)))}]))))

(defn note-display
  [props]
  (let [{:keys [noteName cents octave updateValue]
         {:keys [textField]} :classes} (js->clj props :keywordize-keys true)]
    [(r/adapt-react-class js/MaterialUI.TextField)
     {:label       "note"
      :select      true
      :className   textField
      :value       noteName
      :helperText  " "
      :margin      "normal"
      :onChange    #(updateValue (freq-from-note-name (-> % .-target .-value) octave cents))
      :SelectProps {:readOnly false
                    :style    {:width "7rem"}}}
     (map-indexed (fn [idx note]
                    ^{:key note} [(r/adapt-react-class js/MaterialUI.MenuItem) {:value note} note])
                  note-map)]))

(defn octave-display
  [props]
  (let [{:keys [octave note cents updateValue classes]} (js->clj props :keywordize-keys true)]
    [(r/adapt-react-class js/MaterialUI.TextField)
     {:label "8ve"
      :type "number"
      :className (:textField classes)
      :value octave
      :helperText " "
      :margin "normal"
      :onChange #(updateValue (let [val (-> % .-target .-value)
                                    diff (* 12 (- val octave))]
                                (freq-from-note (+ note diff) cents)))
      :inputProps {:readOnly false
                   :style {:width "2rem"}}}]))

(defn cents-display
  [props]
  (let [state (atom {:edited? false})
        r-state (r/atom {:temp nil
                         :error? false})]
    (fn [props]
      (let [{:keys [note cents updateValue classes]} (js->clj props :keywordize-keys true)]
        [(r/adapt-react-class js/MaterialUI.TextField)
         {:label "cents"
          :type "number"
          :className (:textField classes)
          :value (if (some? (:temp @r-state)) (:temp @r-state) (.toFixed (js/Number. cents) 3))
          :helperText " "
          :margin "normal"
          :inputProps {:readOnly false
                       :style {:width "5rem"}}
          :onChange (fn [ev]
                      (let [new-value (-> ev .-target .-value)]
                        (if (true? (:edited? @state))
                          ((fn []
                             (swap! r-state assoc :temp new-value :error? (js/isNaN (js/parseFloat new-value)))
                             (swap! state assoc :edited? false)))
                          ((fn []
                             (swap! r-state assoc :temp nil :error? false)
                             (updateValue (freq-from-note note new-value)))))))
          :onKeyDown (fn [ev]
                       (let [key (-> ev .-key)
                             new-value (-> ev .-target .-value)]
                         (condp re-matches key
                           #"Enter" :>> (fn [ev]
                                          (swap! r-state assoc :temp nil)
                                          (updateValue (freq-from-note note new-value)))
                           #"([0-9.+-]*)" :>> #(swap! state assoc :edited? true)
                           #"Backspace" :>> #(swap! state assoc :edited? true)
                           #"Delete" :>> #(swap! state assoc :edited? true)
                           ())))
          :onBlur (fn [ev]
                    (swap! r-state assoc :temp nil)
                    (updateValue (freq-from-note note (-> ev .-target .-value))))}]))))

(defn frequency-group
  "Frequency Group component. Since it is wrapped by Material withTheme, the props are js keys. Outer function makes num-display-with-style"
  [props]
  (let [decorator (js/MaterialUI.withStyles display-style)
        num-display-with-style (r/adapt-react-class (decorator (r/reactify-component num-display)))
        note-display-with-style (r/adapt-react-class (decorator (r/reactify-component note-display)))
        octave-display-with-style (r/adapt-react-class (decorator (r/reactify-component octave-display)))
        cents-display-with-style (r/adapt-react-class (decorator (r/reactify-component cents-display)))]
    (fn [props]
      (let [{:keys [tone-id node-id node theme]} props
            clj-node     (js->clj node :keywordize-keys true)
            type         (:type clj-node)
            value        ((keyword type) clj-node)
            slider-props (:slider clj-node)
            width        300
            max-log      (js/Math.log2 (js/parseFloat (:max slider-props)))
            min-log      (js/Math.log2 (js/parseFloat (:min slider-props)))
            dragging?    (:dragging? slider-props)
            scale        (/ (- max-log min-log) width)
            thumb-left   (value-to-log-position value min-log scale)
            primary-dark (-> theme .-palette .-primary .-dark)
            update-value (partial dispatch-update-value tone-id node-id)
            note         (closest-note value)
            note-name    (closest-note-name value)
            octave       (closest-note-octave value)
            cents        (cents-from-closest-note value)
            common-props {:value value
                          :note note
                          :note-name note-name
                          :octave octave
                          :cents cents
                          :thumb-left thumb-left
                          :slider-props slider-props
                          :color primary-dark
                          :update-value update-value
                          :scale scale
                          :min-log min-log}]
        [:div.slider-container
         (use-style (container-style dragging?))
         [(r/adapt-react-class js/MaterialUI.Typography) {:variant "headline"} (str (capitalize type) ":")]
         [slider (merge common-props {:width width})
          [thumb (merge common-props {:key "thumb"})]
          [:div.track-before (use-style (track-before-style thumb-left primary-dark) {:key "track-before"})]
          [:div.track-after (use-style (track-after-style thumb-left width dragging?) {:key "track-after"})]]
         [num-display-with-style {:type         type
                                  :value        value
                                  :slider-props slider-props
                                  :update-value update-value}]
         [note-display-with-style common-props]
         [octave-display-with-style common-props]
         [cents-display-with-style common-props]]))))

(defn volume-group
  [props]
  (let [decorator (js/MaterialUI.withStyles display-style)
        num-display-with-style (r/adapt-react-class (decorator (r/reactify-component num-display)))]
    (fn [props]
      (let [{:keys [tone-id node-id node theme]} props
            clj-node     (js->clj node :keywordize-keys true)
            type         (:type clj-node)
            value        ((keyword type) clj-node)
            slider-props (:slider clj-node)
            width        300
            dragging?    (:dragging? slider-props)]
        [:div
         (use-style (container-style dragging?))
         [(r/adapt-react-class js/MaterialUI.Typography) {:variant "headline"} (str (capitalize type) ":")]]))))

(defn slider-group-with-theme [tone-id node-id node]
  (let [fg (r/adapt-react-class ((js/MaterialUI.withTheme) (r/reactify-component frequency-group)))
        vg (r/adapt-react-class ((js/MaterialUI.withTheme) (r/reactify-component volume-group)))]
    (fn [tone-id node-id node]
      (js/console.log (:type node))
      (condp = (:type node)
        :frequency [fg {:toneid tone-id :node-id node-id :node node}]
        :volume [vg {:toneid tone-id :node-id node-id :node node}]
        ()))))

(defn multiplier-slider
  [])