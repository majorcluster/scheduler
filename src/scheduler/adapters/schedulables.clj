(ns scheduler.adapters.schedulables
  (:require
   [clj-data-adapter.core :as data-adapter]
   [clojure.string :as cstr]
   [scheduler.adapters.commons :as a.commons])
  (:import
   (java.time ZoneOffset)))

(defn in->internal
  [wire]
  (-> wire
      (#(if (:id %)
            %
            (assoc % :id (random-uuid))))
      (#(data-adapter/transform-keys (partial data-adapter/kebab-key->namespaced-key "schedulable") %))))

(defn internal->sql-wire
  [wire]
  (->> (data-adapter/transform-keys #(->> %
                                          name
                                          cstr/lower-case
                                          ((fn [s] (cstr/replace s #"-" "_")))
                                          keyword)
                                    wire)
       (a.commons/update-when-not-nil->> :created_at a.commons/timestamp->sql-timestamp)))

(defn sql-wire->internal
  [wire]
  (-> (data-adapter/transform-keys #(->> %
                                         name
                                         cstr/lower-case
                                         ((fn [s] (cstr/replace s #"_" "-")))
                                         ((fn [s] (str "schedulable/" s)))
                                         keyword)
                                   wire)
      (a.commons/update-when-not-nil :schedulable/created-at #(cond (number? %) %
                                                                    :else (-> %
                                                                              a.commons/inst->local-date-time
                                                                              (.toInstant (ZoneOffset/UTC))
                                                                              (.toEpochMilli))))))
