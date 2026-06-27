(ns doctorkotik.dmjktiad.web.controllers.jungler
  (:require
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [doctorkotik.dmjktiad.cache :as cache]
   [doctorkotik.dmjktiad.services.riot-api :as riot-api]
   [doctorkotik.dmjktiad.services.analysis :as analysis]
   [doctorkotik.dmjktiad.services.verdict :as verdict]))

(defn- safe-profile-icon-id [id]
  (when (integer? id)
    id))

(defn- fetch-and-compute [region game-name tag-line puuid account]
  (let [ids-result (riot-api/get-match-ids region puuid)]
    (if (:error ids-result)
      (do (log/warn "match-ids failed" {:region region :game-name game-name :error (:error ids-result)})
          ids-result)
      (let [match-ids (:ok ids-result)
            matches (keep :ok (map #(riot-api/get-match region %) match-ids))
            drake-games (riot-api/extract-all-drake-data matches puuid)
            stats (analysis/compute-stats drake-games (count matches))
            summoner-result (riot-api/get-summoner region puuid)
            profile-icon-id (safe-profile-icon-id
                             (:profileIconId (:ok summoner-result)))
            league-result (riot-api/get-league region puuid)
            rank-info (when-let [league-entry (when (:ok league-result)
                                                 (some #(when (= (:queueType %) "RANKED_SOLO_5x5") %) (:ok league-result)))]
                        (let [wins (:wins league-entry)
                              losses (:losses league-entry)
                              total (+ wins losses)]
                          {:tier (:tier league-entry)
                           :rank (:rank league-entry)
                           :lp (:leaguePoints league-entry)
                           :wins wins
                           :losses losses
                           :win-rate (if (pos? total) (/ wins total) 0.0)}))
            safe-top-champs (map (fn [[champ count]]
                                   (let [name (str/replace (or champ "") #"[^a-zA-Z0-9._\-\s]" "")
                                         pct (if (pos? (:jungle-games stats))
                                                (float (/ count (:jungle-games stats)))
                                                0.0)]
                                     [name count pct]))
                                 (:top-champs stats))
            not-jungler? (let [rate (:jungle-main-rate stats)]
                           (or (nil? rate) (< rate 0.4)))
            verdict-text (if not-jungler? nil (verdict/verdict stats))]
        {:ok (cond-> (assoc stats
                    :gameName (:gameName account)
                    :tagLine (:tagLine account)
                    :region region
                    :profile-icon-id profile-icon-id
                    :top-champs safe-top-champs
                    :rank rank-info
                    :verdict verdict-text)
              not-jungler? (assoc :not-jungler true))}))))

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
       (do (log/info "check-player" {:player (str game-name "#" tag-line) :cached true :rank (get-in entry [:result :rank :tier])})
           {:ok (merge {:insufficient-data? false
                    :total-drake-kills 0
                    :avg-drakes 0.0
                    :drake-differential 0.0
                    :first-drake-rate 0.0
                    :zero-drake-rate 0.0
                    :team-drake-rate 0.0
                    :control-pct nil
                    :wr-overall nil
                    :wr-with-drakes nil
                    :wr-without-drakes nil
                    :wr-first-drake nil
                    :wr-no-first-drake nil
                    :wr-won-race nil
                    :wr-lost-race nil
                    :wr-tied-race nil
                    :verdict-gap 0.0
                    :drake-deficit-rate 0.0
                    :wr-by-tier []
                    :ranked-games 0
                    :region region}
                   (:result entry)
                   {:cached-at (java.util.Date. (:cached-at entry))})
           :cached true}))
     (let [account-result (riot-api/get-account region game-name tag-line)]
       (if (:error account-result)
         account-result
         (let [puuid (:puuid (:ok account-result))
               account (:ok account-result)
               result (fetch-and-compute region game-name tag-line puuid account)]
           (if (:ok result)
             (let [now (System/currentTimeMillis)
                   result-with-timestamp (assoc (:ok result) :cached-at (java.util.Date. now))
                   rank (get-in result-with-timestamp [:rank :tier])]
               (cache/put region game-name tag-line result-with-timestamp)
               (log/info "check-player" {:player (str game-name "#" tag-line) :cached false :rank rank})
               {:ok result-with-timestamp :cached false})
             result)))))))
