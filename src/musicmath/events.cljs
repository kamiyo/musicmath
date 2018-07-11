(ns musicmath.events
  (:require [re-frame.core   :refer [reg-event-db reg-event-fx after]]
            [cljs.spec.alpha :as s]
            [musicmath.db    :refer [default-db path-to-node]]
            [musicmath.defs  :refer [clamp]]))

(defn check-and-throw
  "Checks db against spec"
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw :musicmath.db/db)))

(reg-event-fx
 :initialize-db
 [check-spec-interceptor]
 (fn [_ _]
   {:db default-db}))

(reg-event-db
 :update-value
 [check-spec-interceptor]
 (fn [db [_ tone-id node-id new-value]]
   (let [path                  (path-to-node tone-id node-id)
         curr-node             (get-in db path)
         type                  (:type curr-node)
         slider                (:slider curr-node)
         parsed-new-value      (js/parseFloat new-value)
         clamped-value         (clamp
                                (if (js/isNaN parsed-new-value) ((keyword type) curr-node) parsed-new-value)
                                (js/parseFloat (:min slider))
                                (js/parseFloat (:max slider)))
         db-with-updated-input (update-in db (path-to-node tone-id node-id :slider) assoc :input (.toPrecision (js/Number. clamped-value) 6))]
     (if (= type :power) (update-in db-with-updated-input (path-to-node tone-id node-id :power) assoc :n clamped-value)
         (update-in db-with-updated-input path assoc type clamped-value)))))

(reg-event-db
 :update-dragging
 [check-spec-interceptor]
 (fn [db [_ is-dragging idx]]
   (update-in db [:tones (int idx)] assoc :is-dragging? is-dragging)))
