(ns lore.core
  (:gen-class)
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))

(defn respond-hello [request]
  {:status 200 :body "Hello, world!"})

(def routes
  (route/expand-routes
   #{["/" :get respond-hello :route-name :greet]}))

(defn create-server [port]
  (http/create-server
   {::http/routes        routes
    ::http/host          "0.0.0.0"
    ::http/type          :jetty
    ::http/port          port
    ::http/resource-path "public"}))

(defn -main [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "8080"))]
    (http/start (create-server port))))
