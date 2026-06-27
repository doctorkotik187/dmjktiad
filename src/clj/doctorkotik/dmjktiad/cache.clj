(ns doctorkotik.dmjktiad.cache)

(defonce store (atom {}))

(defn cache-key [region game-name tag-line]
  [(keyword region) game-name tag-line])

(defn lookup [region game-name tag-line]
  (let [key (cache-key region game-name tag-line)]
    (get @store key)))

(defn put [region game-name tag-line result]
  (let [key (cache-key region game-name tag-line)]
    (swap! store assoc key
           {:result result
            :cached-at (System/currentTimeMillis)})))

(defn invalidate [region game-name tag-line]
  (swap! store dissoc (cache-key region game-name tag-line)))
