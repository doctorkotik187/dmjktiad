(ns doctorkotik.dmjktiad.services.verdict)

(defn- pct [v]
  (when v (int (* v 100))))

(defn verdict
  "Takes the stats map from analysis/compute-stats and returns a roast string."
  [stats]
  (let [gap (:verdict-gap stats)
        cp (:control-pct stats)
        fdr (:first-drake-rate stats)
        zdr (:zero-drake-rate stats)
        dd (:drake-differential stats)
        wr-with (:wr-with-drakes stats)
        wr-without (:wr-without-drakes stats)
        top (:top-champs stats)
        hec? (= (ffirst top) "Hecarim")]

    (let [special (cond
                    (and gap (> gap 0.40) cp (< cp 0.40))
                    "Statistically aware that drakes win games. Philosophically opposed to getting them."

                    (and fdr (< fdr 0.15))
                    "First drake goes to: the enemy. Every game. Like clockwork. Almost impressive."

                    (and zdr (> zdr 0.40))
                    "In over 40% of games their team got zero drakes. The dragon spawned. And died. For someone else."

                    (and dd (< dd -1.2))
                    "The enemy jungler is sending their regards. And their drake stack."

                    (and wr-without (> wr-without 0.52) cp (< cp 0.42))
                    "Wins games without drakes somehow. Like a man who thrives despite himself. Don't let it fool you."

                    (and cp (> cp 0.58) wr-with (< wr-with 0.52))
                    "Gets the drakes. Loses anyway. This is a different kind of problem."

                    hec?
                    "Hecarim main detected. The inting is load-bearing. Drake was not on the way."

                    :else nil)

          primary (cond
                    (nil? cp) "Not enough data to judge. But probably not great."
                    (< cp 0.30) "There is a drake on the map!? Huh?!"
                    (< cp 0.40) "Drakes were harmed in the making of these losses. By the enemy."
                    (< cp 0.48) "Aware that drakes exist. Deeply conflicted about engaging with them."
                    (< cp 0.55) "Sometimes checks drake. Like a man checking his ex's Instagram."
                    (< cp 0.63) "Solid drake priority. Chat is still mad about something else."
                    :else "This jungler eats drakes for breakfast. Respect.")

          suffix (cond
                   (and gap (> gap 0.45))
                   (str "WR jumps from " (pct wr-without) "% to " (pct wr-with) "% with drakes. The data doesn't lie.")

                   (and gap (> gap 0.30))
                   "The gap between 'got drakes' and 'didn't' is not subtle."

                   (and gap (< gap 0.10))
                   "Oddly, drakes don't seem to matter much here. Investigate further."

                   :else nil)]

      (if special
        (if suffix (str special " " suffix) special)
        (if suffix (str primary " " suffix) primary)))))
