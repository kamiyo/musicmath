(ns musicmath.defs)

(def my-blue {:rgb "rgb(78, 134, 164)" :hex "#4e86a4"})

(def note-map ["A" "A#/Bb" "B" "C" "C#/Db" "D" "D#/Eb" "E" "F" "F#/Gb" "G" "G#/Ab"])

(defn closest-note
  [pitch]
  (js/Math.round (+ 49 (* (js/Math.log2 (/ pitch 440)) 12))))

(defn closest-note-name
  [pitch]
  (let [number (js/Math.round (+ 49 (* (js/Math.log2 (/ pitch 440)) 12)))
        char (note-map (mod (- number 49) 12))]
    char))

(defn freq-from-note
  [note]
  (* 440 (js/Math.pow 2 (/ (- note 49) 12))))

(defn cents [a b]
  (cond
    (< a b) (str "+" (.toFixed (js/Number. (* 1200 (js/Math.log2 (/ b a)))) 3))
    (> a b) (str "-" (.toFixed (js/Number. (* 1200 (js/Math.log2 (/ a b)))) 3))
    (= a b) "+0.000"))

(defn cents-from-closest-note [pitch]
  (cents (freq-from-note (closest-note pitch)) pitch))