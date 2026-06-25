(ns doctorkotik.dmjktiad.web.controllers.jungler
  (:require
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [doctorkotik.dmjktiad.cache :as cache]
   [doctorkotik.dmjktiad.services.riot-api :as riot-api]
   [doctorkotik.dmjktiad.services.analysis :as analysis]
   [doctorkotik.dmjktiad.services.verdict :as verdict]))

(defn- compute-stats [drake-data]
  (let [total-games (count drake-data)
        games-with-drakes (filter #(pos? (:drake-kills %)) drake-data)
        drake-rate (if (pos? total-games)
                     (float (/ (count games-with-drakes) total-games))
                     0.0)
        wins-with-drakes (filter :win games-with-drakes)
        wins-without-drakes (filter :win (remove #(pos? (:drake-kills %)) drake-data))
        wr-with-drakes (if (seq games-with-drakes)
                         (float (/ (count wins-with-drakes) (count games-with-drakes)))
                         0.0)
        wr-without-drakes (if (seq (remove #(pos? (:drake-kills %)) drake-data))
                            (float (/ (count wins-without-drakes)
                               (count (remove #(pos? (:drake-kills %)) drake-data))))
                            0.0)
        total-drake-kills (reduce + (map :drake-kills drake-data))
        avg-drakes (if (pos? total-games)
                     (float (/ total-drake-kills total-games))
                     0.0)
        first-drake-games (filter :drake-first drake-data)
        first-drake-rate (if (pos? total-games)
                          (float (/ (count first-drake-games) total-games))
                          0.0)
        drake-distribution (frequencies (map :drake-kills drake-data))
        top-champs (->> drake-data
                        (group-by :champion)
                        (map (fn [[champ games]] [champ (count games)]))
                        (sort-by second >)
                        (take 3))]
    {:total-games     total-games
     :jungle-games    total-games
     :drake-rate      drake-rate
     :total-drake-kills total-drake-kills
     :avg-drakes      avg-drakes
     :first-drake-rate first-drake-rate
     :drake-distribution drake-distribution
     :win-with-drakes   wr-with-drakes
     :win-without-drakes wr-without-drakes
     :top-champs     top-champs
     :verdict-key     (verdict/rate->key drake-rate)}))

(defn- safe-champ-name [champ]
  (when (string? champ)
    (str/replace champ #"[^a-zA-Z0-9._\-\s]" "")))

(defn- safe-profile-icon-id [id]
  (when (integer? id)
    id))

(defn- fetch-and-compute [region game-name tag-line puuid account]
  (let [ids-result (riot-api/get-match-ids region puuid 100)]
    (if (:error ids-result)
      (do (log/warn "match-ids failed" {:region region :game-name game-name :error (:error ids-result)})
          ids-result)
      (let [match-ids (:ok ids-result)
            matches (keep :ok (map #(riot-api/get-match region %) match-ids))
            drake-data (analysis/extract-all-drake-data matches puuid)
            stats (compute-stats drake-data)
            summoner-result (riot-api/get-summoner region puuid)
            profile-icon-id (safe-profile-icon-id
                             (:profileIconId (:ok summoner-result)))
            safe-top-champs (map (fn [[champ count]]
                                   [(safe-champ-name champ) count])
                                 (:top-champs stats))]
        (log/info "player stats" {:region region
                                  :player (str game-name "#" tag-line)
                                  :ranked-games (count matches)
                                  :jungle-games (:total-games stats)
                                  :total-drake-kills (:total-drake-kills stats)
                                  :avg-drakes (:avg-drakes stats)
                                  :drake-rate (:drake-rate stats)
                                  :first-drake-rate (:first-drake-rate stats)
                                  :wr-with-drakes (:win-with-drakes stats)
                                  :wr-without-drakes (:win-without-drakes stats)
                                  :verdict (:verdict-key stats)})
        {:ok (assoc stats
                    :gameName (:gameName account)
                    :tagLine (:tagLine account)
                    :profile-icon-id profile-icon-id
                    :top-champs safe-top-champs
                    :ranked-games (count matches)
                    :jungle-percentage (if (pos? (count matches))
                                         (* 100.0 (/ (:total-games stats) (count matches)))
                                         0)
                    :verdict (verdict/get-verdict (:drake-rate stats)))}))))

(def cooldown-ms (* 60 60 1000))

(defn check-player
  "Full pipeline: resolve account -> fetch matches -> filter jungle -> compute stats.
  Returns {:ok stats-map :cached bool :rate-limited bool :retry-in-ms ms} or {:error reason}."
  ([region game-name tag-line]
   (check-player region game-name tag-line false))
  ([region game-name tag-line force-refresh?]
   (if-let [entry (cache/lookup region game-name tag-line)]
     (if (and force-refresh? (:cached-at entry) (< (- (System/currentTimeMillis) (:cached-at entry)) cooldown-ms))
       {:ok (assoc (:result entry) :cached-at (java.util.Date. (:cached-at entry))) :cached true :rate-limited true :retry-in-ms (- cooldown-ms (- (System/currentTimeMillis) (:cached-at entry)))}
       {:ok (merge {:total-drake-kills 0
                    :avg-drakes 0.0
                    :first-drake-rate 0.0
                    :drake-distribution {}
                    :ranked-games 0
                    :jungle-percentage 0.0}
                   (:result entry)
                   {:cached-at (java.util.Date. (:cached-at entry))})
           :cached true})
     (let [account-result (riot-api/get-account region game-name tag-line)]
       (if (:error account-result)
         account-result
         (let [puuid (:puuid (:ok account-result))
               account (:ok account-result)
               result (fetch-and-compute region game-name tag-line puuid account)]           (if (:ok result)
             (let [now (System/currentTimeMillis)
                   result-with-timestamp (assoc (:ok result) :cached-at (java.util.Date. now))]
               (cache/put region game-name tag-line result-with-timestamp)
               {:ok result-with-timestamp :cached false})
             result)))))))
