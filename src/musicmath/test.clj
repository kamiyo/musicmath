(ns test
  (:require [spec-provider.provider :as sp]))

(def inferred-specs
  (sp/infer-specs {:tones
                   [{:nodes
                     [{:type :pitch
                       :pitch 440
                       :slider {:dragging? false
                                :input "440"
                                :bypass? false}}
                      {:type :volume
                       :volume 1.0
                       :slider {:dragging? false
                                :input "1.0"
                                :bypass? false}}
                      {:type :partial
                       :partial 0
                       :slider {:dragging? true
                                :input "0"
                                :bypass? false}}
                      {:type :power
                       :power {:ratio_num 2.0
                               :ratio_den 1.0
                               :n 1}
                       :slider {:dragging? true
                                :input "0"
                                :bypass? false}}]}]}
                  :test/db))

inferred-specs

(sp/pprint-specs inferred-specs 'test 's)