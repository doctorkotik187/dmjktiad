(ns doctorkotik.dmjktiad.web.middleware.core
  (:require
    [doctorkotik.dmjktiad.env :as env]
    [ring.middleware.defaults :as defaults]
    [ring.middleware.session.cookie :as cookie]))

(defn- wrap-csp [handler]
  (fn [request]
    (let [response (handler request)]
      (-> response
          (assoc-in [:headers "Content-Security-Policy"]
                    (str "default-src 'self'; "
                         "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
                         "script-src 'self' https://unpkg.com; "
                         "img-src 'self' data: https://ddragon.leagueoflegends.com; "
                         "connect-src 'self'; "
                         "frame-ancestors 'none'"))
          (assoc-in [:headers "X-Content-Type-Options"] "nosniff")
          (assoc-in [:headers "X-Frame-Options"] "DENY")))))

(defn wrap-base
  [{:keys [metrics site-defaults-config cookie-secret] :as opts}]
  (let [cookie-store (cookie/cookie-store {:key (.getBytes ^String cookie-secret)})]
    (fn [handler]
      (cond-> ((:middleware env/defaults) handler opts)
              true (defaults/wrap-defaults
                     (assoc-in site-defaults-config [:session :store] cookie-store))
              true wrap-csp))))
