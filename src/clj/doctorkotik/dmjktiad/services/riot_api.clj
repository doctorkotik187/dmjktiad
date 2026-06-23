(ns doctorkotik.dmjktiad.services.riot-api
  (:require
   [clojure.string :as str]
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
      (throw (ex-info "RIOT_API_KEY env var not set" {}))))

(defn- get-request [url]
  (let [response (http/get url {:headers {"X-Riot-Token" (api-key)}
                                 :as :json})]
    (case (:status response)
      200 {:ok (:body response)}
      400 {:error :bad-request}
      401 {:error :unauthorized}
      403 {:error :forbidden}
      404 {:error :not-found}
      429 {:error :rate-limited}
      {:error :server-error})))

(defn get-account
  "Resolves a player by Riot ID. Returns {:ok {:puuid ... :gameName ... :tagLine ...}} or {:error reason}."
  [region game-name tag-line]
  (let [cluster (region->cluster region)
        url (str (base-url cluster)
                 "/riot/account/v1/accounts/by-riot-id/"
                 (java.net.URLEncoder/encode (name game-name) "UTF-8")
                 "/"
                 (java.net.URLEncoder/encode (name tag-line) "UTF-8"))]
    (get-request url)))

(defn get-match-ids
  "Fetches recent ranked match IDs for a puuid. Returns {:ok [id1 id2 ...]} or {:error reason}."
  ([region puuid]
   (get-match-ids region puuid 20))
  ([region puuid count]
   (let [cluster (region->cluster region)
         url (str (base-url cluster)
                  "/lol/match/v5/matches/by-puuid/"
                  (java.net.URLEncoder/encode puuid "UTF-8")
                  "/ids?count=" count "&queue=420")]
     (get-request url))))

(defn get-match
  "Fetches a single match by ID. Returns {:ok match-map} or {:error reason}."
  [region match-id]
  (let [cluster (region->cluster region)
        url (str (base-url cluster)
                 "/lol/match/v5/matches/"
                 (java.net.URLEncoder/encode (name match-id) "UTF-8"))]
    (get-request url)))
