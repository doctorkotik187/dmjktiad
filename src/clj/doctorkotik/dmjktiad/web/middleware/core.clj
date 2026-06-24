(ns doctorkotik.dmjktiad.web.middleware.core
  (:require
    [doctorkotik.dmjktiad.env :as env]
    [ring.middleware.defaults :as defaults]
    [ring.middleware.session.cookie :as cookie]))

(def ^:private security-headers
  {"Content-Security-Policy"
   (str "default-src 'self'; "
        "script-src 'self' https://unpkg.com; "
        "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; "
        "img-src 'self' https://ddragon.leagueoflegends.com data:; "
        "connect-src 'self'; "
        "frame-ancestors 'none'; "
        "base-uri 'self'; "
        "form-action 'self'")
   "X-Content-Type-Options" "nosniff"
   "X-Frame-Options" "DENY"
   "X-XSS-Protection" "1; mode=block"})

(defn wrap-base
  [{:keys [metrics site-defaults-config cookie-secret] :as opts}]
  (let [cookie-store (cookie/cookie-store {:key (.getBytes ^String cookie-secret)})]
    (fn [handler]
      (cond-> ((:middleware env/defaults) handler opts)
              true (defaults/wrap-defaults
                     (assoc-in site-defaults-config [:session :store] cookie-store))
              true (fn [request]
                     (let [response (handler request)]
                       (update response :headers merge security-headers)))))))
