(defproject lore "0.1.0-SNAPSHOT"
  :description "A learning management system"
  :url "https://github.com/thiagooak/lore"
  :dependencies [[org.clojure/clojure        "1.12.0"]
                 [io.pedestal/pedestal.jetty "0.7.2"]
                 [org.slf4j/slf4j-simple     "2.0.17"]]

  :main lore.core
  :uberjar-name "lore-standalone.jar"
  :min-lein-version "2.0.0"
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
