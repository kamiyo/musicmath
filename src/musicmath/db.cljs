(ns musicmath.db
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :as rf]))

(s/def ::id int?)
(s/def ::playing? boolean?)
(s/def ::base-pitch float?)
(s/def ::ratio float?)
(s/def ::input string?)
(s/def ::multiplier int?)
(s/def ::editing? boolean?)
(s/def ::dragging? boolean?)
(s/def ::tone (s/keys :req-un [::base-pitch
                               ::ratio
                               ::editing?
                               ::input
                               ::multiplier
                               ::dragging?
                               ::playing?]))
(s/def ::tones (s/* ::tone))
(s/def ::db (s/keys :req-un [::tones]))

(def default-db
  {:tones [{:base-pitch 440
            :ratio 2
            :multiplier 1
            :input "1"
            :dragging? false
            :editing? false
            :playing? false}]})