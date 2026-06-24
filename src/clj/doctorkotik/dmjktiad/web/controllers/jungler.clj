(ns doctorkotik.dmjktiad.web.controllers.jungler
  (:require
   [doctorkotik.dmjktiad.services.riot-api :as riot-api]
   [doctorkotik.dmjktiad.services.analysis :as analysis]
   [doctorkotik.dmjktiad.services.verdict :as verdict]))

(defn- compute-stats [drake-data]
  (let [total-games (count drake-data)
        games-with-drakes (filter #(pos? (:drake-kills %)) drake-data)
        drake-rate (if (pos? total-games)
                     (/ (count games-with-drakes) total-games)
                     0)
        wins-with-drakes (filter :win games-with-drakes)
        wins-without-drakes (filter :win (remove #(pos? (:drake-kills %)) drake-data))
        wr-with-drakes (if (seq games-with-drakes)
                         (/ (count wins-with-drakes) (count games-with-drakes))
                         nil)
        wr-without-drakes (if (seq (remove #(pos? (:drake-kills %)) drake-data))
                            (/ (count wins-without-drakes)
                               (count (remove #(pos? (:drake-kills %)) drake-data)))
                            nil)
        top-champs (->> drake-data
                        (group-by :champion)
                        (map (fn [[champ games]] [champ (count games)]))
                        (sort-by second >)
                        (take 3))]
    {:total-games     total-games
     :jungle-games    total-games
     :drake-rate      drake-rate
     :win-with-drakes   wr-with-drakes
     :win-without-drakes wr-without-drakes
     :top-champs     top-champs
     :verdict-key     (verdict/rate->key drake-rate)}))

(defn check-player
  "Full pipeline: resolve account -> fetch matches -> filter jungle -> compute stats.
  Returns {:ok stats-map} or {:error reason}."
  [region game-name tag-line]
  (let [account-result (riot-api/get-account region game-name tag-line)]
    (if (:error account-result)
      account-result
      (let [puuid (:puuid (:ok account-result))
            account (:ok account-result)
            ids-result (riot-api/get-match-ids region puuid 20)]
        (if (:error ids-result)
          ids-result
          (let [match-ids (:ok ids-result)
                matches (keep :ok (map #(riot-api/get-match region %) match-ids))
                drake-data (analysis/extract-all-drake-data matches puuid)
                stats (compute-stats drake-data)]
            {:ok (assoc stats
                        :gameName (:gameName account)
                        :tagLine (:tagLine account)
                        :verdict (verdict/get-verdict (:drake-rate stats)))}))))))
