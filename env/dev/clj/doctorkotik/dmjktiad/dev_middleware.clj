(ns doctorkotik.dmjktiad.dev-middleware)

(defn wrap-no-cache [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Cache-Control"] "no-cache, no-store, must-revalidate"))))

(defn wrap-dev [handler _opts]
  (-> handler
      wrap-no-cache))
