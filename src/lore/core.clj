(ns lore.core
  (:require [clojure.data.json :as json]
            [io.pedestal.connector :as conn]
            [io.pedestal.http.http-kit :as hk]
            [io.pedestal.http.content-negotiation :as content-negotiation])
  (:gen-class))

(def port (or (some-> (System/getenv "PORT")
                      parse-long)
              8080))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def accepted (partial response 202))


(def supported-types ["text/html"
                      "application/edn"
                      "application/json"
                      "text/plain"])

(def content-negotiation-interceptor
  (content-negotiation/negotiate-content supported-types))

(defn accepted-type
  [context]
  (get-in context [:request :accept :field] "text/plain"))

(defn transform-content
  [body content-type]
  (case content-type
    "text/html" body
    "text/plain" body
    "application/edn" (pr-str body)
    "application/json" (json/write-str body)))

(defn coerce-to
  [response content-type]
  (-> response
      (update :body transform-content content-type)
      (assoc-in [:headers "Content-Type"] content-type)))

(defn missing-response-content-type?
  [context]
  (nil? (get-in context [:response :headers "Content-Type"])))

(def coerce-body-interceptor
  {:name ::coerce-body
   :leave
   (fn [context]
     (cond-> context
       (missing-response-content-type? context)
       (update :response coerce-to (accepted-type context))))})

(defn greet-handler [_request]
  {:status 200
   :body   "Hello, world!\n"})

(def echo
  {:name  :echo
   :enter #(assoc % :response (ok (:request %)))})

(def routes
  #{["/" :get [coerce-body-interceptor
               content-negotiation-interceptor
               greet-handler] :route-name :greet]
    ["/echo" :get [coerce-body-interceptor
                   content-negotiation-interceptor
                   echo] :route-name :echo]})

(defn create-connector []
  (-> (conn/default-connector-map "0.0.0.0" port)
      (conn/with-default-interceptors)
      (conn/with-routes routes)
      (hk/create-connector nil)))

(defn -main [& _args]
  (conn/start! (create-connector)))

;; DEV

(defonce *connector (atom nil))

(defn start []
  (reset! *connector
          (conn/start! (create-connector))))

(defn stop []
  (conn/stop! @*connector)
  (reset! *connector nil))

(defn restart []
  (stop)
  (start))

(comment
  (start)
  (restart)
  (stop))
