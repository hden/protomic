(ns protomic.core
  (:refer-clojure :exclude [sync])
  (:require [clojure.core.async :as a :refer [go <!]]
            [cognitect.anomalies :as anomalies]
            [datomic.client.api.async :as d]
            [promesa.core :as p]))

(defn- error? [x]
  (and
    (map? x)
    (::anomalies/category x)))

(defn- error-map->ex-info [m]
  (let [message (get m ::anomalies/message "Unknown Anomaly.")]
    (ex-info message m)))

(defn- wrap-one
  [f]
  (fn [& args]
    (p/create
      (fn [resolve reject]
        (go
          (try
            (let [value-or-error (<! (apply f args))]
              (if (error? value-or-error)
                (reject (error-map->ex-info value-or-error))
                (resolve value-or-error)))
            (catch Throwable ex
              (reject ex))))))))

(defn- wrap-many
  [f]
  (fn [& args]
    (p/create
      (fn [resolve reject]
        (go
          (try
            (let [chunks (<! (a/into [] (apply f args)))
                  value-or-error (first chunks)]
              (if (error? value-or-error)
                (reject (error-map->ex-info value-or-error))
                (resolve (mapcat identity chunks))))
            (catch Throwable ex
              (reject ex))))))))

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
