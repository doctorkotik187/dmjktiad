(ns doctorkotik.dmjktiad.env
  (:require
    [clojure.tools.logging :as log]
    [doctorkotik.dmjktiad.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[dmjktiad starting using the development or test profile]=-"))
   :start      (fn []
                 (log/info "\n-=[dmjktiad started successfully using the development or test profile]=-"))
   :stop       (fn []
                 (log/info "\n-=[dmjktiad has shut down successfully]=-"))
   :middleware wrap-dev
   :opts       {:profile       :dev}})
