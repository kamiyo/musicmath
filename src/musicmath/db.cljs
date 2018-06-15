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
(s/def ::pitch (s/and number? pos? #(<= 8000)))
(s/def ::partial integer?)
(s/def ::power (s/keys :req-un [::ratio_den ::ratio-num ::n]))
(s/def ::volume double?)
(s/def ::type #{:pitch :volume :partial :power})
(s/def ::nodes (s/coll-of (s/keys :req-un [::slider ::type] :opt-un [::pitch ::partial ::power ::volume])))
(s/def ::tones (s/coll-of (s/keys :req-un [::nodes])))
(s/def
  ::db
  (s/keys :req-un [::tones]))

(defn default-pitch-slider [pitch]
  {:dragging? false
   :input (str pitch)
   :max "8000"
   :min "15"
   :step "any"
   :bypass? false})

(defn default-pitch-node [pitch]
  {:type :pitch
   :pitch pitch
   :slider (default-pitch-slider pitch)})

(def default-db
  {:tones
   [{:nodes
     [(default-pitch-node 440)]}]})

(defn path-to-node [tone-id node-id & fields]
  (if (nil? fields)
    [:tones (int tone-id) :nodes (int node-id)]
    (into [:tones (int tone-id) :nodes (int node-id)] fields)))