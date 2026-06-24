(ns doctorkotik.dmjktiad.services.analysis)

(defn- jungle-game?
  "Checks if a participant played jungle (has Smite + JUNGLE teamPosition)."
  [participant]
  (and (= (:teamPosition participant) "JUNGLE")
       (or (= (:summoner1Id participant) 11)
           (= (:summoner2Id participant) 11))))

(defn- find-participant
  "Finds the target player's participant data in a match by puuid."
  [match puuid]
  (some #(when (= (:puuid %) puuid) %) (:participants (:info match))))

(defn- team-objectives
  "Gets the objectives map for a given team (100 or 200) from a match."
  [match team-id]
  (-> (:info match)
      (:teams)
      (->> (filter #(= (:teamId %) team-id))
      first
      :objectives)))

(defn extract-drake-data
  "Extracts drake-related data for a single confirmed jungle game.
  Returns nil if the player didn't jungle in this match."
  [match puuid]
  (when-let [participant (find-participant match puuid)]
    (when (jungle-game? participant)
      (let [team-id (:teamId participant)
            objs (team-objectives match team-id)
            dragon (:dragon objs)]
        {:match-id     (:matchId (:metadata match))
         :win          (:win participant)
         :team-id      team-id
         :drake-kills  (:kills dragon)
         :drake-first  (:first dragon)
         :champion     (:championName participant)
         :game-duration (:gameDuration (:info match))}))))

(defn extract-all-drake-data
  "Filters matches to jungle games and extracts drake data from each."
  [matches puuid]
  (keep #(extract-drake-data % puuid) matches))
