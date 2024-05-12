(ns scheduler.controllers.schedulables
  (:require
   [scheduler.controllers.dates :as c.dates]
   [scheduler.ports.sql.repositories.schedulables :as r.schedulables]))

(defn create
  [model]
  (let [model (-> model
                  (assoc :schedulable/created-at
                         (c.dates/current-date)))
        unique? (-> model
                    :schedulable/name
                    r.schedulables/find-by-name
                    empty?)]
    (if unique?
      (r.schedulables/insert! model)
      (throw (ex-info "Uniqueness Failure" {:type :duplicated
                                            :message "A schedulable with that name already exists"})))))
