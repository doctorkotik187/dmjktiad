(ns doctorkotik.dmjktiad.services.verdict)

(defn- pct [v]
  (when v (int (* v 100))))

(defn verdict
  "Takes the stats map from analysis/compute-stats and returns a roast string."
  [stats]
  (let [gap        (:verdict-gap stats)
        cp         (:control-pct stats)
        fdr        (:first-drake-rate stats)
        zdr        (:zero-drake-rate stats)
        dd         (:drake-differential stats)
        wr-with    (:wr-with-drakes stats)
        wr-without (:wr-without-drakes stats)
        top        (:top-champs stats)
        top-champ  (ffirst top)]
    (let [special
          (cond
            (= top-champ "Nidalee")
            "Nidalee main detected. Please reconsider your life choices. And your champion pool."

            (= top-champ "Hecarim")
            "Hecarim main. He was going to run it down. The drake was also not on the way."

            (and gap (> gap 0.40) cp (< cp 0.40))
            "Knows drakes win games. Has decided this is someone else's problem."

            (and fdr (< fdr 0.15))
            "First drake rate: nearly zero. The enemy jungler has been there every time. Comfortable."

            (and zdr (> zdr 0.40))
            "Zero drakes in 40%+ of games. The pit just sat there. Untouched. Mocking."

            (and dd (< dd -1.2))
            "Losing the drake race by over one objective per game. The enemy jungler is not stressed."

            (and wr-without (> wr-without 0.52) cp (< cp 0.42))
            "Winning without drakes somehow. Do not let this fool either of you."

            (and cp (> cp 0.58) wr-with (< wr-with 0.52))
            "Full drake control. Still losing. This is a different conversation."

            :else nil)

          primary
          (cond
            (nil? cp)   "Not enough data. But the vibes are not good."
            (< cp 0.30) "There is a drake on the map!? Huh?!"
            (< cp 0.40) "The enemy team has been securing your drakes for you. Every game."
            (< cp 0.48) "Drake control: optional. Apparently."
            (< cp 0.55) "Shows up to drake sometimes. Results vary."
            (< cp 0.63) "Solid drake presence. Chat is upset about something else."
            :else       "Actually knows what a drake is. And paths to it. Rare.")

          suffix
          (cond
            (and gap (> gap 0.45))
            (str "WR goes from " (pct wr-without) "% to " (pct wr-with) "% with drakes. The numbers are not subtle.")

            (and gap (> gap 0.30))
            "Drake games and non-drake games tell very different stories here."

            (and gap (< gap 0.10))
            "Drakes don't seem to be the issue. Keep looking."

            :else nil)]

      (if special
        (if suffix (str special " " suffix) special)
        (if suffix (str primary " " suffix) primary)))))
