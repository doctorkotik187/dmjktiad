(ns doctorkotik.dmjktiad.web.middleware.rate-limit)

(defonce store (atom {}))

(def window-ms (* 60 60 1000))
(def max-requests 30)

(defn- cleanup [now]
  (swap! store #(into {} (filter (fn [[_ v]] (> (:last-at v) (- now window-ms))) %))))

(defn check [now ip]
  (cleanup now)
  (let [entry (get @store ip)
        entry (or entry {:count 0 :last-at now})
        count (:count entry)
        last-at (:last-at entry)
        elapsed (- now last-at)]
    (if (and (> count 0) (< elapsed window-ms) (>= count max-requests))
      {:allowed false :retry-in-ms (- window-ms elapsed)}
      (do
        (swap! store assoc ip {:count (inc count) :last-at (if (>= elapsed window-ms) now last-at)})
        {:allowed true}))))
