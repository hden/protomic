(ns protomic.core-test
  (:require [clojure.test :as t :refer [deftest testing is are]]
            [datomic.client.api :as d]
            [promesa.core :as p]
            [protomic.core :as core]))

(def client
  (d/client {:server-type :dev-local
             :storage-dir :mem
             :system      "dev"}))

(d/create-database client {:db-name "dev"})
(def conn (d/connect client {:db-name "dev"}))
;; Test Schema
(d/transact
  conn
  {:tx-data
   [{:db/ident       :system/id
     :db/valueType   :db.type/string
     :db/cardinality :db.cardinality/one
     :db/unique      :db.unique/identity}

    {:db/ident       :system/value
     :db/valueType   :db.type/long
     :db/cardinality :db.cardinality/one}]})
;; Test Data
(d/transact
  conn
  {:tx-data
   (into []
         (map (fn [x]
                {:system/id (str x) :system/value x}))
         (range 100))})

(defn tap [x]
  (println "tap>" x)
  x)

(def shared-args {:chunk 10})

(def not-nil? (complement nil?))

(deftest client-test
  (testing "create then connect to the database."
    (let [db-name "new-db"]
      (is (= true @(core/create-database client {:db-name db-name})))
      (is (= 2 (count @(core/list-databases client shared-args))))
      (is @(core/connect client {:db-name db-name}))
      (is (= true @(core/delete-database client {:db-name db-name}))))))

(deftest db-test
  (testing "db-returning functions"
    (are [f args] (not-nil? @(apply f args))
      core/sync [conn 1]
      core/with-db [conn])))

(deftest reader-test
  (let [db (d/db conn)]
    (testing "map-returning functions"
      (are [f args] (map? @(apply f args))
        core/db-stats [db]
        core/pull [db {:selector '[*] :eid [:system/id "0"]}]))

    (testing "sequence-returning functions"
      (are [f args] (not-empty @(f db (merge shared-args args)))
        core/datoms      {:index :eavt}
        core/index-pull  {:index :avet :selector '[*] :start [:system/id 0]}
        core/index-range {:attrid :system/id :start 0}))

    (testing "q"
      (are [f args] (not-empty @(f (merge shared-args args)))
        core/q    {:args [db] :query '[:find (pull ?e [*]) :where [?e :system/id _]]}
        core/qseq {:args [db] :query '[:find (pull ?e [*]) :where [?e :system/id _]]}))

    (testing "tx-range"
      (is (not-empty @(core/tx-range conn shared-args))))))

(deftest writer-test
  (testing "transact"
    (let [result @(core/transact conn {:tx-data [{:db/id "foobar" :system/id "foobar" :system/value 0}]})]
      (are [key pred] (pred (get result key))
        :db-before not-nil?
        :db-after not-nil?
        :tx-data not-empty
        :tempids map?))))

(deftest error-handling-test
  (testing "errors from reader"
    (let [db (d/db conn)]
      (is (thrown-with-msg?
            java.util.concurrent.ExecutionException
            #"Unable to resolve entity"
            @(core/pull db {:selector '[*] :eid [:foo/bar "baz"]})))))

  (testing "errors from transaction"
    (is (thrown-with-msg?
          java.util.concurrent.ExecutionException
          #"Unable to resolve entity"
          @(core/transact conn {:tx-data [{:foo/bar "baz"}]})))))

(deftest sample-code-test
  (testing "sample code from readme"
    (let [results @(-> (core/connect client {:db-name "dev"})
                    (p/chain
                      d/db
                      #(core/q {:args [%]
                                :query '[:find ?tx-inst
                                         :where [_ :db/txInstant ?tx-inst]]})
                      #(map first %)))]
      (is (every? inst? results)))))
