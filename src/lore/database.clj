(ns lore.database
  (:require [datomic.api :as d]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn read-file [s]
  (with-open [r (io/reader (io/resource s))]
    (edn/read-string (slurp r))))

(defn dev-conn []
  (d/connect "datomic:mem://lore"))

(defn dev-reset []
  (let [uri "datomic:mem://lore"
        conn (do (d/delete-database uri)
                 (d/create-database uri)
                 (d/connect uri))]
    @(d/transact conn (read-file "db/schema.edn"))
    @(d/transact conn (read-file "db/seed.edn"))
    conn))

(comment
  (dev-reset)
  (def conn (dev-conn))

  @(d/transact (dev-conn) [{:user/email "email@gmail.com"
                            :user/personas {:db/id [:persona/type "Learner"]}}])

  (d/q '[:find ?e ?aa ?vv
         :where
         [?e :user/email "email@gmail.com"]
         [?e :user/personas ?v]
         [?v ?aa ?vv]]
       (d/db (dev-conn))))