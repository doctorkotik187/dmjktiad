(ns doctorkotik.dmjktiad.web.routes.pages
  (:require
    [clojure.string :as str]
    [doctorkotik.dmjktiad.web.controllers.jungler :as jungler]
    [doctorkotik.dmjktiad.web.middleware.exception :as exception]
    [doctorkotik.dmjktiad.web.pages.layout :as layout]
    [integrant.core :as ig]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]))

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
        [game-name tag-line] (str/split riot-id #"#" 2)
        result (jungler/check-player (keyword region) game-name tag-line)]
    (if (:error result)
      (layout/render request "error.html" {:status 400
                                           :title "Error"
                                           :message (name (:error result))})
      (layout/render request "result.html" (:ok result)))))

;; Routes
(defn page-routes [_opts]
  [["/" {:get home}]
   ["/check" {:post check-handler}]])

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
