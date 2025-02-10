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

(defn has-name-and-not-unique
  [to-update]
  (and (:schedulable/name to-update)
       (-> to-update
           :schedulable/name
           r.schedulables/find-by-name
           not-empty)))

(defn update
  [to-update]
  (let [model (r.schedulables/find-by-id! (:schedulable/id to-update))]
    (cond
      (nil? model)
      (throw (ex-info "Not Found Failure" {:type :not-found
                                           :message "A schedulable with that id was not found"}))

      (<= (count to-update) 1)
      (throw (ex-info "Minimum Attributes Failure" {:type :min-attrs
                                                    :message "Provide at least one attribute to change"}))

      (has-name-and-not-unique to-update)
      (throw (ex-info "Uniqueness Failure" {:type :duplicated
                                            :message "A schedulable with that name already exists"}))

      :else
      (r.schedulables/update! (merge model to-update)
                              (:schedulable/id to-update)))))

(defn all
  []
  (r.schedulables/find-all))

(defn by-id
  [id]
  (if (not (nil? id))
   (or (r.schedulables/find-by-id! id)
       (throw (ex-info "Not Found Failure" {:type :not-found
                                            :message "A schedulable with that id was not found"})))
   (throw (ex-info "Not Found Failure" {:type :not-found
                                        :message "A schedulable with that id was not found"}))))
