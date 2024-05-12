(ns scheduler.controllers.dates
  (:import
   [java.util Date]))

(defn current-date []
  (.getTime (Date.)))
