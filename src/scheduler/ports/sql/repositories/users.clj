(ns scheduler.ports.sql.repositories.users
  (:require
   [next.jdbc.sql :as jdbc]
   [scheduler.adapters.users :as a.users]
   [scheduler.ports.sql.core :as sql.c]
   [scheduler.ports.sql.repositories.entities :as r.entities]))

(defn find-all
  []
  (-> (r.entities/find-all "users")
      a.users/sql-wire->internal))

(defn print-> [x msg]
  (println msg x)
  x)

(defn find-by-email
  [email]
  (-> sql.c/datasource
      (jdbc/query ["select * from users where email = ?" email])
      first
      a.users/sql-wire->internal))

(defn find-by-id
  [id]
  (->> id
       (r.entities/find-by-id "users")
       a.users/sql-wire->internal
       (#(if (empty? %) nil %))))

(defn insert!
  [m]
  (r.entities/insert! :users (a.users/internal->sql-wire m)))

(defn update!
  [m id]
  (r.entities/update! :users
                      (a.users/internal->sql-wire m)
                      ["id = ?" id]))

(defn delete-by-id!
  [id]
  (r.entities/delete-by-id! :users (parse-long id)))
