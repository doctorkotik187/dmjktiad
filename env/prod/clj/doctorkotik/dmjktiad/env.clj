(ns doctorkotik.dmjktiad.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[dmjktiad starting]=-"))
   :start      (fn []
                 (log/info "\n-=[dmjktiad started successfully]=-"))
   :stop       (fn []
                 (log/info "\n-=[dmjktiad has shut down successfully]=-"))
   :middleware (fn [handler _] handler)
   :opts       {:profile :prod}})
