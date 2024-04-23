(ns scheduler.ports.core
  (:require
   [scheduler.ports.http-in.core :as in.ports]
   [scheduler.ports.http-out.core :as out.ports]
   [scheduler.ports.sql.core :as sql.c]))

(defn start-ports-dev
  [migrate?]
  (sql.c/start migrate?)
  (in.ports/start-dev)
  (out.ports/start))

(defn start-ports
  [migrate?]
  (sql.c/start migrate?)
  (in.ports/start)
  (out.ports/start))
