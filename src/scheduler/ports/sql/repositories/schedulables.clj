(ns scheduler.ports.sql.repositories.schedulables
  (:require
   [next.jdbc.sql :as jdbc]
   [scheduler.adapters.schedulables :as a.schedulables]
   [scheduler.ports.sql.core :as sql.c]
   [scheduler.ports.sql.repositories.entities :as r.entities]))

(defn find-all
  []
  (-> (r.entities/find-all "schedulables")
      a.schedulables/sql-wire->internal))

(defn find-by-interval
  [from to]
  (-> sql.c/datasource
      (jdbc/query ["select * from schedulables where created_at >= ? AND created_at < ?" from to])
      a.schedulables/sql-wire->internal))

(defn find-by-id
  [id]
  (->> id
       (r.entities/find-by-id "schedulables")
       a.schedulables/sql-wire->internal
       (#(if (empty? %) nil %))))

(defn insert!
  [m]
  (r.entities/insert! :schedulables (a.schedulables/internal->sql-wire m)))

(defn update!
  [m id]
  (r.entities/update! :schedulables
                      (a.schedulables/internal->sql-wire m)
                      ["id = ?" id]))

(defn delete-by-id!
  [id]
  (r.entities/delete-by-id! :schedulables (parse-long id)))
