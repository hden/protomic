(ns protomic.core
  (:refer-clojure :exclude [sync transduce])
  (:require [clojure.core.async :as a :refer [go transduce <!]]
            [cognitect.anomalies :as anomalies]
            [datomic.client.api.async :as d]
            [promesa.core :as p]))

(defn- error? [x]
  (or
    (instance? Throwable x)
    (and
      (map? x)
      (::anomalies/category x))))

(defn- error->ex-info [x]
  (cond
    ;; passthrough
    (instance? clojure.lang.ExceptionInfo x)
    x

    ;; converts to ExceptionInfo when possible
    (instance? Throwable x)
    (let [m (Throwable->map x)]
      (ex-info (:cause m) m x))

    (map? x)
    (let [message (get x ::anomalies/message "Unknown anomaly.")]
      (ex-info message x))

    :else
    (ex-info "Unknown anomaly." {::anomalies/message  "Unknown anomaly."
                                 ::anomalies/category ::anomalies/fault})))

(defn- drop-after [pred]
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result input]
       (if (pred input)
         (reduced (rf result input))
         (rf result input))))))

(defn- wrap-one
  [f]
  (fn [& args]
    (p/create
      (fn [resolve reject]
        (go
          (try
            (let [value-or-error (<! (apply f args))]
              (if (error? value-or-error)
                (reject (error->ex-info value-or-error))
                (resolve value-or-error)))
            (catch Throwable ex
              (reject (error->ex-info ex)))))))))

(defn- wrap-many
  [f]
  (fn [& args]
    (p/create
      (fn [resolve reject]
        (go
          (try
            (let [chunks (<! (transduce (drop-after error?) conj [] (apply f args)))
                  ;; If there is an error it will be in the channel instead of the chunk.
                  ;; https://docs.datomic.com/client-api/datomic.client.api.async.html
                  chunk-or-error (last chunks)]
              (if (error? chunk-or-error)
                (reject (error->ex-info chunk-or-error))
                (resolve (mapcat identity chunks))))
            (catch Throwable ex
              (reject (error->ex-info ex)))))))))

(def connect (wrap-one d/connect))
(def create-database (wrap-one d/create-database))
(def datoms (wrap-many d/datoms))
(def db-stats (wrap-one d/db-stats))
(def delete-database (wrap-one d/delete-database))
(def index-pull (wrap-one d/index-pull))
(def index-range (wrap-many d/index-range))
(def list-databases (wrap-one d/list-databases))
(def pull (wrap-one d/pull))
(def q (wrap-many d/q))
(def qseq (wrap-many d/qseq))
(def sync (wrap-one d/sync))
(def transact (wrap-one d/transact))
(def tx-range (wrap-many d/tx-range))
(def with-db (wrap-one d/with-db))
