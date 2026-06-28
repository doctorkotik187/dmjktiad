(ns doctorkotik.dmjktiad.config
  (:require
    [aero.core :as aero]
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [kit.config :as config]))

(def ^:const system-filename "system.edn")

(defn system-config
  [options]
  (config/read-config system-filename options))

(defn secrets []
  (let [f (io/file "/app/resources/secrets.edn")]
    (log/info "Loading secrets from" (.getAbsolutePath f))
    (aero/read-config f)))
