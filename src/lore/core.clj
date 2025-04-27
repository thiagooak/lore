(ns lore.core
  (:gen-class)
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [datomic.api :as d]))

(defn connect-to-datomic []
  (def db-uri "datomic:dev://localhost:4334/lore")
  (d/create-database db-uri)
  (d/connect db-uri))

(defn create-database-entities [conn]

  ;; @(d/transact conn [{:persona/type "Manager"}
  ;;                    {:persona/type "Learner"}
  ;;                    {:persona/type "Buddy"}])

  @(d/transact conn [{:db/ident :user/email
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/unique :db.unique/identity}

                     {:db/ident :persona/type
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/unique :db.unique/identity}

                     {:db/ident :user/personas
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/many}

                     {:db/ident :journey/name
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}

                     {:db/ident :journey/description
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}

                     {:db/ident :course/name
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}

                     {:db/ident :course/description
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}

                     {:db/ident :course/journey
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/many}

                     {:db/ident :module/name
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}

                     {:db/ident :module/course
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/many}

                     {:db/ident :objective/description
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}

                     {:db/ident :course/objective
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/many}

                     {:db/ident :user/objective
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/many}

                     {:db/ident :feedback/from
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/one}

                     {:db/ident :feedback/to
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/one}]))

(defn respond-hello [request]
  ;; (d/q '[:find ?e ?aa ?vv
  ;;        :where
  ;;        [?e :user/email "email@gmail.com"]
  ;;        [?e :user/personas ?v]
  ;;        [?v ?aa ?vv]]
  ;;      (d/db conn))
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
