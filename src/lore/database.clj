(ns lore.database
  (:require [datomic.api :as d]))

(defn create-types [conn]
  @(d/transact conn [{:persona/type "Manager"}
                     {:persona/type "Learner"}
                     {:persona/type "Buddy"}]))

(defn create-database-entities [conn]
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

(defn dev-conn []
  (d/connect "datomic:dev://localhost:4334/lore"))

(defn dev-reset []
  (d/delete-database "datomic:dev://localhost:4334/lore")
  (d/create-database "datomic:dev://localhost:4334/lore")
  (create-database-entities (dev-conn))
  (create-types (dev-conn)))

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