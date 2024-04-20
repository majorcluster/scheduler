(ns scheduler.adapters.appointments
  (:require
   [clj-data-adapter.core :as data-adapter]
   [scheduler.adapters.commons :as a.commons]
   [scheduler.commons :as commons]))

(defn internal->sql-wire
  [wire]
  (->> (a.commons/update-when-not-nil wire :created_at a.commons/str->sql-timestamp)
       (#(a.commons/update-when-not-nil %  :scheduled_to a.commons/str->sql-timestamp))
       (data-adapter/transform-keys #(-> % data-adapter/kebab-key->snake-str keyword))))

(defn sql-wire->internal
  [wire]
  (-> wire
      commons/remove-namespace
      (a.commons/update-when-not-nil :created_at a.commons/sql-timestamp->str)
      (a.commons/update-when-not-nil :scheduled_to a.commons/sql-timestamp->str)))
