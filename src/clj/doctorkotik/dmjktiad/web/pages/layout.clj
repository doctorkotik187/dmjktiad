(ns doctorkotik.dmjktiad.web.pages.layout
  (:require
   [clojure.java.io]
   [clojure.string :as str]
   [selmer.parser :as parser]
   [ring.util.http-response :refer [content-type ok]]
   [ring.util.anti-forgery :refer [anti-forgery-field]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
   [ring.util.response]))

(def selmer-opts {:custom-resource-path (clojure.java.io/resource "html")})

(def version
  (str/trim (slurp (clojure.java.io/resource "VERSION"))))

(defn init-selmer!
  [{:keys [env]}]
  (when (= :dev env) (parser/cache-off!))
  (parser/add-tag! :csrf-field (fn [_ _] (anti-forgery-field)))
  (parser/add-filter! :pct (fn [v] (if v (str (int (* v 100)) "%") "N/A")))
  (parser/add-filter! :f1 (fn [v] (when v (format "%.1f" (float v)))))
  (parser/add-filter! :mul (fn [v m] (when (number? v) (* v m)))))

(defn render
  [request template & [params]]
  (-> (parser/render-file template
                          (assoc params :page template :csrf-token *anti-forgery-token* :version version)
                          selmer-opts)
      (ok)
      (content-type "text/html; charset=utf-8")))

(defn error-page
  "error-details should be a map containing the following keys:
   :status - error status
   :title - error title (optional)
   :message - detailed error message (optional)
   returns a response map with the error page as the body
   and the status specified by the status key"
  [error-details]
  {:status  (:status error-details)
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file "error.html" error-details selmer-opts)})