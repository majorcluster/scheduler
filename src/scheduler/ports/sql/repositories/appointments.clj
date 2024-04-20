(ns scheduler.ports.sql.repositories.appointments
  (:require
   [next.jdbc.sql :as jdbc]
   [scheduler.adapters.appointments :as a.appointments]
   [scheduler.ports.sql.core :as sql.c]
   [scheduler.ports.sql.repositories.entities :as r.entities]))

(defn find-all
  []
  (-> (r.entities/find-all "appointments")
      a.appointments/sql-wire->internal))

(defn find-by-interval
  [from to]
  (-> sql.c/datasource
      (jdbc/query ["select * from appointments where created_at >= ? AND created_at < ?" from to])
      a.appointments/sql-wire->internal))

(defn find-by-id
  [id]
  (->> id
       (r.entities/find-by-id "appointments")
       a.appointments/sql-wire->internal
       (#(if (empty? %) nil %))))

(defn insert!
  [m]
  (r.entities/insert! :appointments (a.appointments/internal->sql-wire m)))

(defn update!
  [m id]
  (r.entities/update! :appointments
                      (a.appointments/internal->sql-wire m)
                      ["id = ?" id]))

(defn delete-by-id!
  [id]
  (r.entities/delete-by-id! :appointments (parse-long id)))
