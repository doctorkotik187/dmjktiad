(ns doctorkotik.dmjktiad.core
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [doctorkotik.dmjktiad.config :as config]
   [doctorkotik.dmjktiad.env :refer [defaults]]

    ;; Edges
   [kit.edge.server.undertow]
   [doctorkotik.dmjktiad.web.handler]

    ;; Routes
   [doctorkotik.dmjktiad.web.routes.api] 
    [doctorkotik.dmjktiad.web.routes.pages] 
    [kit.edge.http.hato])
  (:gen-class))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (fn [thread ex]
   (log/error {:what :uncaught-exception
               :exception ex
               :where (str "Uncaught exception on" (.getName thread))})))

(defonce system (atom nil))

(defn stop-app []
  ((or (:stop defaults) (fn [])))
  (some-> (deref system) (ig/halt!)))

(defn start-app [& [params]]
  ((or (:start params) (:start defaults) (fn [])))
  (->> (config/system-config (or (:opts params) (:opts defaults) {}))
       (ig/expand)
       (ig/init)
       (reset! system)))

(defn -main [& _]
  (start-app)
  (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (stop-app) (shutdown-agents)))))
