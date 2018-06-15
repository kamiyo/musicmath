(ns musicmath.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx after]]
            [cljs.spec.alpha :as s]
            [musicmath.db :refer [default-db path-to-node]]))

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
   (let [path (path-to-node tone-id node-id)
         curr-node (get-in db path)
         type (:type curr-node)
         slider (:slider curr-node)
         clamped-value (min (js/parseFloat (:max slider)) (max (js/parseFloat (:min slider)) new-value))
         db-with-updated-input (update-in db (path-to-node tone-id node-id :slider) assoc :input (.toPrecision (js/Number. clamped-value) 6))]
     (js/console.log clamped-value)
     (if (= type :power) (update-in db-with-updated-input (path-to-node tone-id node-id :power) assoc :n clamped-value)
         (update-in db-with-updated-input path assoc type clamped-value)))))

(reg-event-db
 :update-dragging
 [check-spec-interceptor]
 (fn [db [_ is-dragging idx]]
   (update-in db [:tones (int idx)] assoc :is-dragging? is-dragging)))

(reg-event-fx
 :update-input
 [check-spec-interceptor]
 (fn [{:keys [db]} [_ tone-id node-id new-input]]
   (let [path (path-to-node tone-id node-id)
         curr-node (get-in db path)
         slider (:slider curr-node)
         res (merge {:db (update-in db (path-to-node tone-id node-id :slider) assoc :input new-input)}
                    (when (and
                           (number? (js/parseFloat new-input))
                           (let [float-input (js/parseFloat new-input)]
                             (and
                              (> float-input (:min slider))
                              (< float-input (:max slider)))))
                      {:dispatch [:update-value tone-id node-id (js/parseFloat new-input)]}))]
     res)))
