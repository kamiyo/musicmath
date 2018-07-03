(ns musicmath.db
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :as rf]))

(s/def ::bypass? boolean?)
(s/def ::input string?)
(s/def ::dragging? boolean?)
(s/def ::max #(number? (int %)))
(s/def ::min #(number? (int %)))
(s/def ::step (or #(number? (int %)) #(= "any" %)))
(s/def ::slider (s/keys :req-un [::bypass?
                                 ::dragging?
                                 ::input
                                 ::max
                                 ::min
                                 ::step]))
(s/def ::n (s/and pos-int? #(<= % 12)))
(s/def ::ratio_den double?)
(s/def ::ratio_num double?)
(s/def ::frequency (s/and number? pos? #(<= 8000)))
(s/def ::partial integer?)
(s/def ::power (s/keys :req-un [::ratio_den ::ratio-num ::n]))
(s/def ::volume double?)
(s/def ::type #{:frequency :volume :partial :power})
(s/def ::nodes (s/coll-of (s/keys :req-un [::slider ::type] :opt-un [::frequency ::partial ::power ::volume])))
(s/def ::tones (s/coll-of (s/keys :req-un [::nodes])))
(s/def
  ::db
  (s/keys :req-un [::tones]))

(defn default-frequency-slider [frequency]
  {:dragging? false
   :input (str frequency)
   :max "7902.132820098001"
   :min "15.43385316425384"
   :step "any"
   :bypass? false})

(defn default-frequency-node [frequency]
  {:type :frequency
   :frequency frequency
   :slider (default-frequency-slider frequency)})

(def default-db
  {:tones
   [{:nodes
     [(default-frequency-node 440)]}]})

(defn path-to-node [tone-id node-id & fields]
  (if (nil? fields)
    [:tones (int tone-id) :nodes (int node-id)]
    (into [:tones (int tone-id) :nodes (int node-id)] fields)))