(ns doctorkotik.dmjktiad.config
  (:require
    [aero.core :as aero]
    [clojure.java.io :as io]
    [kit.config :as config]))

(def ^:const system-filename "system.edn")

(defn system-config
  [options]
  (config/read-config system-filename options))

(defn secrets []
  (aero/read-config (clojure.java.io/resource "secrets.edn")))
