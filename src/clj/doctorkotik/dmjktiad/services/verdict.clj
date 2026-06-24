(ns doctorkotik.dmjktiad.services.verdict)

(def verdicts
  {:clueless   "There is a dragon?!"
   :suspicious "Bold of you to assume they check the map."
   :aware      "Aware of dragons. Motivated by them: unclear."
   :decent     "Decent drake presence. Might even ping it."
   :based      "This jungler eats drakes for breakfast. Respect."})

(defn rate->key [drake-rate]
  (cond
    (<= drake-rate 0.2) :clueless
    (<= drake-rate 0.4) :suspicious
    (<= drake-rate 0.6) :aware
    (<= drake-rate 0.8) :decent
    :else :based))

(defn get-verdict
  "Takes a drake rate (0.0-1.0) and returns the roast string."
  [drake-rate]
  (get verdicts (rate->key drake-rate)))
