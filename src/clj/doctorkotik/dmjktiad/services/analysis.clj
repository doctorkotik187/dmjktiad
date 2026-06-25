(ns doctorkotik.dmjktiad.services.analysis)

(defn- safe-div [num denom]
  (if (zero? denom) nil (/ (float num) denom)))

(defn- mean [coll]
  (if (empty? coll)
    nil
    (/ (reduce + coll) (float (count coll)))))

(defn- wr [games]
  (when (seq games)
    (/ (count (filter :win games)) (float (count games)))))

(defn compute-stats
  "Takes a seq of jungle game maps and total ranked games.
   Returns a comprehensive stats map."
  [games total-games]
  (let [jungle-games (count games)
        insufficient? (< jungle-games 15)
        team-drakes (map :team-drakes games)
        enemy-drakes (map :enemy-drakes games)
        sum-team (reduce + team-drakes)
        sum-enemy (reduce + enemy-drakes)
        avg-team (safe-div sum-team jungle-games)
        avg-enemy (safe-div sum-enemy jungle-games)
        drake-diff (- (or avg-team 0.0) (or avg-enemy 0.0))
        first-drake-games (filter :first-drake games)
        zero-drake-games (filter #(zero? (:team-drakes %)) games)
        with-drakes-games (filter #(pos? (:team-drakes %)) games)
        without-drakes-games (filter #(zero? (:team-drakes %)) games)
        won-race-games (filter #(> (:team-drakes %) (:enemy-drakes %)) games)
        lost-race-games (filter #(< (:team-drakes %) (:enemy-drakes %)) games)
        tied-race-games (filter #(= (:team-drakes %) (:enemy-drakes %)) games)
        no-first-drake-games (filter #(not (:first-drake %)) games)
        control-fractions (keep identity
                                (map (fn [g]
                                       (let [t (:team-drakes g)
                                             e (:enemy-drakes g)
                                             sum (+ t e)]
                                         (when (pos? sum)
                                           (/ (float t) sum))))
                                     games))
        control-pct (mean control-fractions)
        wr-overall (wr games)
        wr-with (wr with-drakes-games)
        wr-without (wr without-drakes-games)
        wr-first (wr first-drake-games)
        wr-no-first (wr no-first-drake-games)
        wr-won (wr won-race-games)
        wr-lost (wr lost-race-games)
        wr-tied (wr tied-race-games)
        verdict-gap (- (or wr-with 0.0) (or wr-without 0.0))
        deficit-rate (safe-div (count lost-race-games) jungle-games)
        tier-0 (filter #(= (:team-drakes %) 0) games)
        tier-1-2 (filter #(<= 1 (:team-drakes %) 2) games)
        tier-3 (filter #(= (:team-drakes %) 3) games)
        tier-4+ (filter #(>= (:team-drakes %) 4) games)
        tier-wr (fn [g] (when (seq g) (/ (count (filter :win g)) (float (count g)))))
        top-champs (->> games
                        (group-by :champion)
                        (map (fn [[champ g]] [champ (count g)]))
                        (sort-by second >)
                        (take 3))]
    {:total-games total-games
     :jungle-games jungle-games
     :jungle-main-rate (safe-div jungle-games total-games)
     :insufficient-data? insufficient?
     :avg-team-drakes avg-team
     :avg-enemy-drakes avg-enemy
     :drake-differential drake-diff
     :first-drake-rate (safe-div (count first-drake-games) jungle-games)
     :zero-drake-rate (safe-div (count zero-drake-games) jungle-games)
     :team-drake-rate (safe-div (count with-drakes-games) jungle-games)
     :control-pct control-pct
     :wr-overall wr-overall
     :wr-with-drakes wr-with
     :wr-without-drakes wr-without
     :wr-first-drake wr-first
     :wr-no-first-drake wr-no-first
     :wr-won-race wr-won
     :wr-lost-race wr-lost
     :wr-tied-race wr-tied
     :verdict-gap verdict-gap
     :drake-deficit-rate deficit-rate
     :wr-by-tier [{:label "0 drakes"   :min 0 :max 0  :wr (tier-wr tier-0)   :games (count tier-0)   :wr-pct (some-> (tier-wr tier-0) (* 100) int)}
                  {:label "1-2 drakes" :min 1 :max 2  :wr (tier-wr tier-1-2) :games (count tier-1-2) :wr-pct (some-> (tier-wr tier-1-2) (* 100) int)}
                  {:label "3 drakes"   :min 3 :max 3  :wr (tier-wr tier-3)   :games (count tier-3)   :wr-pct (some-> (tier-wr tier-3) (* 100) int)}
                  {:label "4+ (Soul)"  :min 4 :max 99 :wr (tier-wr tier-4+)  :games (count tier-4+)  :wr-pct (some-> (tier-wr tier-4+) (* 100) int)}]
     :top-champs top-champs}))
