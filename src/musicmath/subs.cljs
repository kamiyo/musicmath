(ns musicmath.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
 :number-of-tones
 (fn [db _]
   (count (:tones db))))

(reg-sub
 :multiplier
 (fn [db idx]
   (get-in db [:tones (int idx) :multiplier])))

(reg-sub
 :input
 (fn [db idx]
   (get-in db [:tones (int idx) :input])))

(reg-sub
 :dragging?
 (fn [db idx]
   (get-in db [:tones (int idx) :dragging?])))

(reg-sub
 :editing?
 (fn [db idx]
   (get-in db [:tones (int idx) :editing?])))