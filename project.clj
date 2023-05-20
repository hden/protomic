(defproject com.github.hden/protomic "0.2.1-SNAPSHOT"
  :description "Async client API for Datomic with Promesa."
  :url "https://github.com/hden/protomic"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :managed-dependencies [[com.datomic/client-cloud "1.0.123"]
                         [funcool/promesa "11.0.664"]]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.cognitect/anomalies "0.1.12"]
                 [com.datomic/client-cloud]
                 [diehard "0.11.7"]
                 [funcool/promesa]]
  :repl-options {:init-ns protomic.core}
  :profiles
  {:dev {:dependencies [[com.datomic/dev-local "0.9.232"]]}})
