(ns musicmath.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx after]]
            [cljs.spec.alpha :as s]
            [musicmath.db :refer [default-db]]))

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
 :update-multiplier
 [check-spec-interceptor]
 (fn [db [_ new-multiplier idx]]
   (let [multiplier-int (max 0 (min 20 (js/parseInt new-multiplier)))]
     (update-in db [:tones (int idx)] assoc :multiplier multiplier-int :input (str multiplier-int)))))

(reg-event-db
 :update-dragging
 [check-spec-interceptor]
 (fn [db [_ is-dragging idx]]
   (update-in db [:tones (int idx)] assoc :is-dragging? is-dragging)))

(reg-event-fx
 :update-input
 [check-spec-interceptor]
 (fn [{:keys [db]} [_ new-input idx]]
   (let [res (merge {:db (update-in db [:tones (int idx)] assoc :input new-input)}
                    (when-not (js/isNaN (js/parseInt new-input))
                      {:dispatch [:update-multiplier new-input idx]}))]
     res)))

(reg-event-db
 :update-editing
 [check-spec-interceptor]
 (fn [db [_ editing idx]]
   (let [tones (:tones db)
         tone (nth tones idx)]
   (update-in db [:tones (int idx)] assoc :editing? editing))))