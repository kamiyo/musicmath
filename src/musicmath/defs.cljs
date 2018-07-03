(ns musicmath.defs)

(def my-blue {:rgb "rgb(78, 134, 164)" :hex "#4e86a4"})

(def note-map ["C" "C#/Db" "D" "D#/Eb" "E" "F" "F#/Gb" "G" "G#/Ab" "A" "A#/Bb" "B"])

(defn index-of-note-name
  [note]
  (first (keep-indexed #(when (= note %2) %1) note-map)))

(defn closest-note
  [freq]
  (js/Math.round (+ 49 (* (js/Math.log2 (/ freq 440)) 12))))

(defn closest-note-name
  [freq]
  (let [number (js/Math.round (+ 49 (* (js/Math.log2 (/ freq 440)) 12)))
        char (note-map (mod (- number 40) 12))]
    (str char)))

(defn closest-note-octave
  [freq]
  (let [number (js/Math.round (+ 49 (* (js/Math.log2 (/ freq 440)) 12)))
        octave (+ 4 (js/Math.floor (/ (- number 40) 12)))]
    octave))

(defn half-step-up
  [freq]
  (let [ratio (js/Math.pow 2 (/ 1 12))]
    (* freq ratio)))

(defn half-step-down
  [freq]
  (let [ratio (js/Math.pow 2 (/ 1 12))]
    (/ freq ratio)))

(defn freq-from-note
  [note]
  (* 440 (js/Math.pow 2 (/ (- note 49) 12))))

(defn freq-from-note-name
  [name octave]
  (js/console.log name octave)
  (js/console.log (index-of-note-name name))
  (* 440 (js/Math.pow 2 (/ (- (+ 40 (index-of-note-name name)) (* 12 (- 4 octave)) 49) 12))))

(defn cents [a b]
  (cond
    (< a b) (str "+" (.toFixed (js/Number. (* 1200 (js/Math.log2 (/ b a)))) 3))
    (> a b) (str "-" (.toFixed (js/Number. (* 1200 (js/Math.log2 (/ a b)))) 3))
    (= a b) "+0.000"))

(defn cents-from-closest-note [pitch]
  (cents (freq-from-note (closest-note pitch)) pitch))