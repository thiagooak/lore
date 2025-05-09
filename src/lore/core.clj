(ns lore.core
  (:require [io.pedestal.connector :as conn]
            [io.pedestal.http.http-kit :as hk])
  (:gen-class))

(def port (or (some-> (System/getenv "PORT")
                      parse-long)
              8080))

(defn greet-handler [_request]
  {:status 200
   :body   "Hello, world!\n"})

(def routes
  #{["/" :get greet-handler :route-name :greet]})

(defn create-connector []
  (-> (conn/default-connector-map "0.0.0.0" port)
      (conn/with-default-interceptors)
      (conn/with-routes routes)
      (hk/create-connector nil)))

(defn -main [& _args]
  (conn/start! (create-connector)))
