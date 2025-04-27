(ns lore.core
  (:gen-class)
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [datomic.api :as d]
            [lore.database :as db]))

(defn respond-hello [request]
  (let [result (d/q '[:find ?email
         :where
         [?p :persona/type "Learner"]
         [?e :user/personas ?p]
         [?e :user/email ?email]]
       (d/db (db/dev-conn)))]
  {:status 200 :body (str result)}))

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
