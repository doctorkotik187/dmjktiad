(ns doctorkotik.dmjktiad.services.riot-api
  (:require
   [clojure.string :as str]
   [doctorkotik.dmjktiad.config :as config]
   [hato.client :as http]))

(def ^:private routing
  {"americas" ["na1" "br1" "la1" "la2"]
   "europe"  ["euw1" "eun1" "tr1" "ru"]
   "asia"    ["kr" "jp1"]
   "sea"     ["oc1" "ph2" "sg2" "th2" "tw2" "vn2"]})

(def ^:private platforms
  (into {} (for [[cluster codes] routing
                 code codes]
             [code cluster])))

(defn region->cluster
  "Maps a platform code (e.g. :euw1) to a routing cluster (e.g. \"europe\").
  Defaults to \"europe\" if the code is unknown."
  [platform]
  (get platforms (str/lower-case (name platform)) "europe"))

(defn- base-url [cluster]
  (str "https://" cluster ".api.riotgames.com"))

(defn- api-key []
  (or (System/getenv "RIOT_API_KEY")
      (:riot-api-key (config/secrets))
      (throw (ex-info "RIOT_API_KEY env var not set and no secrets.edn found" {}))))

(defn- get-request [url]
  (let [response (http/get url {:headers {"X-Riot-Token" (api-key)}
                                 :as :json
                                 :throw-exceptions? false})]
    (case (:status response)
      200 {:ok (:body response)}
      400 {:error :bad-request}
      401 {:error :unauthorized}
      403 {:error :forbidden}
      404 {:error :not-found}
      429 {:error :rate-limited}
      {:error :server-error})))

(defn- url-encode [s]
  (-> (java.net.URLEncoder/encode s "UTF-8")
      (str/replace "+" "%20")))

(defn- find-participant [match puuid]
  (some #(when (= (:puuid %) puuid) %) (:participants (:info match))))

(defn- team-objectives [match team-id]
  (-> (:info match)
      (:teams)
      (->> (filter #(= (:teamId %) team-id))
      first
      :objectives)))

(defn- jungle-game? [participant]
  (and (= (:teamPosition participant) "JUNGLE")
       (or (= (:summoner1Id participant) 11)
           (= (:summoner2Id participant) 11))))

(defn extract-drake-data [match puuid]
  (when-let [participant (find-participant match puuid)]
    (when (jungle-game? participant)
      (let [team-id (:teamId participant)
            objs (team-objectives match team-id)
            dragon (:dragon objs)
            enemy-id (if (= team-id 100) 200 100)
            enemy-objs (team-objectives match enemy-id)
            enemy-dragon (:dragon enemy-objs)]
        {:win          (:win participant)
         :team-drakes  (:kills dragon)
         :enemy-drakes (:kills enemy-dragon)
         :first-drake  (:first dragon)
         :champion     (:championName participant)
         :duration     (:gameDuration (:info match))}))))

(defn extract-all-drake-data [matches puuid]
  (keep #(extract-drake-data % puuid) matches))

(defn get-account
  "Resolves a player by Riot ID. Returns {:ok {:puuid ... :gameName ... :tagLine ...}} or {:error reason}."
  [region game-name tag-line]
  (let [cluster (region->cluster region)
        url (str (base-url cluster)
                 "/riot/account/v1/accounts/by-riot-id/"
                 (url-encode (name game-name))
                 "/"
                 (url-encode (name tag-line)))]
    (get-request url)))

(defn get-match-ids
  "Fetches recent ranked match IDs for a puuid. Returns {:ok [id1 id2 ...]} or {:error reason}."
  ([region puuid]
   (get-match-ids region puuid 20))
  ([region puuid count]
   (let [cluster (region->cluster region)
         url (str (base-url cluster)
                  "/lol/match/v5/matches/by-puuid/"
                  (url-encode puuid)
                  "/ids?count=" count "&queue=420")]
     (get-request url))))

(defn get-summoner
  "Fetches summoner data by puuid. Returns {:ok {:profileIconId ... :summonerLevel ...}} or {:error reason}."
  [region puuid]
  (let [platform (str/lower-case (name region))
        url (str "https://" platform ".api.riotgames.com/lol/summoner/v4/summoners/by-puuid/"
                 (url-encode puuid))]
    (get-request url)))

(defn get-match
  "Fetches a single match by ID. Returns {:ok match-map} or {:error reason}."
  [region match-id]
  (let [cluster (region->cluster region)
        url (str (base-url cluster)
                 "/lol/match/v5/matches/"
                 (url-encode (name match-id)))]
    (get-request url)))
