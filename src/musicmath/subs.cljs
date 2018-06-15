(ns musicmath.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(defn get-helper [tone-id node-id & keywords]
  [:tones (int tone-id) :nodes (int node-id) keywords])

(reg-sub
 :get-tones
 (fn [db _]
   (:tones db)))

(reg-sub
 :get-nodes
 (fn [db tone-id]
   (get-in db [:tones (int tone-id) :nodes])))

(reg-sub
 :number-of-nodes
 (fn [db tone-id]
   (count (get-in db [:tones (int tone-id) :nodes]))))

(reg-sub
 :value
 (fn [db tone-id node-id]
   (get-in db (get-helper tone-id node-id :value))))

(reg-sub
 :input
 (fn [db tone-id node-id]
   (get-in db (get-helper tone-id node-id :slider :input))))

(reg-sub
 :dragging?
 (fn [db tone-id node-id]
   (get-in db (get-helper tone-id node-id :slider :dragging?))))