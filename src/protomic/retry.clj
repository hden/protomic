(ns protomic.retry
  (:require [cognitect.anomalies :as anomalies]
            [diehard.core :as diehard]
            [diehard.util :as u])
  (:import [java.util List]
           [dev.failsafe CircuitBreakerOpenException ExecutionContext Failsafe
                         FailsafeExecutor FailsafeException]
           [dev.failsafe.event ExecutionCompletedEvent]
           [dev.failsafe.function ContextualSupplier]))

(def retryable-anomaly?
  "Set of retryable anomalies."
  #{::anomalies/busy
    ::anomalies/unavailable
    ::anomalies/interrupted})

(defn- retry? [_ e]
  (-> e ex-data ::anomalies/category retryable-anomaly?))

(diehard/defretrypolicy default-retry-policy
  {:max-retries 5
   :backoff-ms [1 1000]
   :retry-if retry?})

(defmacro with-retry [opt & body]
  `(let [the-opt# ~opt
         retry-policy# (diehard/retry-policy-from-config the-opt#)
         fallback# (diehard/fallback the-opt#)
         cb# (:circuit-breaker the-opt#)
         policies# (vec (filter some? [fallback# retry-policy# cb#]))
         failsafe# (Failsafe/with ^List policies#)
         failsafe# (if-let [on-complete# (:on-complete the-opt#)]
                     (.onComplete failsafe#
                                  (u/wrap-event-listener
                                   (fn [^ExecutionCompletedEvent event#]
                                     (diehard/with-context event#
                                       (on-complete# (.getResult event#)
                                                     (.getFailure event#))))))
                     failsafe#)
         callable# (reify ContextualSupplier
                     (get [_# ^ExecutionContext ctx#]
                       (diehard/with-context ctx#
                         ~@body)))]
     (try
       (.getStageAsync ^FailsafeExecutor failsafe# ^ContextualSupplier callable#)
       (catch CircuitBreakerOpenException e#
         (throw e#))
       (catch FailsafeException e#
         (throw (.getCause e#))))))
