(ns doctorkotik.dmjktiad.web.routes.pages
  (:require
    [clojure.string :as str]
    [doctorkotik.dmjktiad.web.controllers.jungler :as jungler]
    [doctorkotik.dmjktiad.web.middleware.exception :as exception]
    [doctorkotik.dmjktiad.web.middleware.rate-limit :as rate-limit]
    [doctorkotik.dmjktiad.web.pages.layout :as layout]
    [integrant.core :as ig]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]))

(def ^:private op-gg-regions
  {"na1" "na" "br1" "br" "la1" "lan" "la2" "las"
   "euw1" "euw" "eun1" "eun" "tr1" "tr" "ru" "ru"
   "kr" "kr" "jp1" "jp" "oc1" "oce"})

(def ^:private riot-regions
  (into {} (map (fn [[k v]] [v k]) op-gg-regions)))

(def ^:private valid-regions
  (set (concat (keys op-gg-regions) (vals op-gg-regions))))

(defn- ->op-gg-region [region]
  (get op-gg-regions region region))

(defn- ->riot-region [region]
  (get riot-regions region region))

(defn- url-path [s]
  (-> (java.net.URLEncoder/encode s "UTF-8") (str/replace "+" "%20")))

(defn wrap-rate-limit [handler]
  (fn [request]
    (let [ip (get-in request [:remote-addr])
          now (System/currentTimeMillis)
          check (rate-limit/check now ip)]
      (if (:allowed check)
        (handler request)
        (-> (layout/render request "error.html" {:status 429
                                                :title "Rate limited"
                                                :message (str "Too many requests. Try again in " (int (Math/ceil (/ (:retry-in-ms check) 60000))) " minutes.")})
            (assoc-in [:headers "Retry-After"] (str (int (Math/ceil (/ (:retry-in-ms check) 1000))))))))))

(defn wrap-page-defaults []
  (let [error-page (layout/error-page
                     {:status 403
                      :title "Invalid anti-forgery token"})]
    #(wrap-anti-forgery % {:error-response error-page})))

(defn home [request]
  (layout/render request "home.html"))

(defn check-handler [request]
  (let [region (get-in request [:form-params "region"])
        riot-id (get-in request [:form-params "riot-id"])
        [game-name tag-line] (str/split riot-id #"#" 2)]
    (if (or (nil? game-name) (nil? tag-line) (empty? game-name) (empty? tag-line)
            (not (valid-regions region))
            (not (re-matches #"^[a-zA-Z0-9._\-\s]+$" game-name))
            (not (re-matches #"^[a-zA-Z0-9._\-\s]+$" tag-line)))
      (layout/render request "error.html" {:status 400
                                           :title "Error"
                                           :message "Invalid Riot ID format"})
      (let [redirect-url (str "/summoners/" (->op-gg-region region) "/" (url-path game-name) "-" (url-path tag-line))]
        {:status 302
         :headers {"Location" redirect-url}
         :body ""}))))

(defn summoner-page [request]
  (let [region (get-in request [:path-params :region])
        full-name (get-in request [:path-params :full-name])
        [game-name tag-line] (when (string? full-name)
                               (str/split full-name #"-" 2))]
    (if (or (nil? game-name) (nil? tag-line) (empty? game-name) (empty? tag-line)
            (not (valid-regions region))
            (not (re-matches #"^[a-zA-Z0-9._\-\s]+$" full-name)))
      (layout/render request "error.html" {:status 400
                                           :title "Error"
                                           :message "Invalid summoner URL"})
      (let [force-refresh? (= (get-in request [:query-params "refresh"]) "true")
            result (jungler/check-player (keyword (->riot-region region)) game-name tag-line force-refresh?)]
        (if (:error result)
          (layout/render request "error.html" {:status (case (:error result)
                                                          :not-found 404
                                                          :bad-request 400
                                                          :unauthorized 403
                                                          :forbidden 403
                                                          :rate-limited 429
                                                          500)
                                               :title (case (:error result)
                                                         :not-found "Player Not Found"
                                                         :bad-request "Invalid Request"
                                                         :unauthorized "Unauthorized"
                                                         :forbidden "Forbidden"
                                                         :rate-limited "Rate Limited"
                                                         "Error")
                                               :message (if-let [api-msg (:message result)]
                                                          api-msg
                                                          (case (:error result)
                                                            :not-found (str "Player " game-name "#" tag-line " was not found on " region ". Check the name and region.")
                                                            :bad-request "The request was invalid. Please try again."
                                                            :unauthorized "Invalid or expired API key."
                                                            :forbidden "The API key may have expired. Please contact the site owner."
                                                            :rate-limited "Too many requests. Please try again later."
                                                            "Something went wrong. Please try again later."))})
          (if (get-in result [:ok :not-jungler])
            (layout/render request "not-jungler.html" {:gameName (get-in result [:ok :gameName])
                                                        :tagLine (get-in result [:ok :tagLine])})
            (let [clean-url (str "/summoners/" region "/" (url-path game-name) "-" (url-path tag-line))
                  template-data (assoc (:ok result)
                                       :cached (:cached result)
                                       :rate-limited (:rate-limited result false)
                                       :retry-in-ms (:retry-in-ms result 0)
                                       :retry-in-seconds (int (Math/ceil (/ (:retry-in-ms result 0) 1000)))
                                       :op-gg-url (str "https://op.gg/lol" clean-url))]
              (if (:rate-limited result)
                (layout/render request "result.html" template-data)
                (if force-refresh?
                  {:status 302
                   :headers {"Location" clean-url}
                   :body ""}
                  (layout/render request "result.html" template-data))))))))))

;; Routes
(defn page-routes [_opts]
  [["/" {:get home}]
   ["/check" {:post (wrap-rate-limit check-handler)}]
   ["/summoners/:region/:full-name" {:get (wrap-rate-limit summoner-page)}]])

(def route-data
  {:middleware
   [(wrap-page-defaults)
    parameters/parameters-middleware
    muuntaja/format-response-middleware
    exception/wrap-exception]})

(derive :reitit.routes/pages :reitit/routes)

(defmethod ig/init-key :reitit.routes/pages
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (layout/init-selmer! opts)
  (fn [] [base-path route-data (page-routes opts)]))
