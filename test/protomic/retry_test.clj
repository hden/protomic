(ns protomic.retry-test
  (:require [clojure.test :as t :refer [deftest testing is]]
            [cognitect.anomalies :as anomalies]
            [promesa.core :as p]
            [promesa.exec :refer [default-executor]]
            [protomic.retry :refer [with-retry default-retry-policy]]))

(defn fail [counter]
  (p/create (fn [resolve reject]
              (if (< 0 (swap! counter dec))
                (reject (ex-info "Oops" {::anomalies/category
                                         ::anomalies/unavailable}))
                (resolve "OK")))
            default-executor))

(deftest retry-test
  (testing "success"
    (let [result (with-retry {:policy default-retry-policy}
                   (p/resolved "OK"))]
      (is (p/promise? result))
      (is (= "OK" @result))))

  (testing "retry, success"
    (let [counter (atom 3)
          result (with-retry {:policy default-retry-policy}
                   (fail counter))]
      (is (p/promise? result))
      (is (= "OK" @result))))

  (testing "retry, failed"
    (let [counter (atom 10)
          result (with-retry {:policy default-retry-policy}
                   (fail counter))]
      (is (p/promise? result))
      (is (thrown-with-msg? java.util.concurrent.ExecutionException #"Oops"
                            @result)))))
