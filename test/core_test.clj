(ns core-test
  (:require
   [clojure.data.json :as json]
   [clojure.string :as cstr]
   [clojure.test :refer :all]
   [io.pedestal.http :as bootstrap]
   [scheduler.ports.http-in.core :as service]
   [scheduler.ports.sql.core :as sql.c]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))

(defn extract-validation-msgs
  [resp]
  (-> resp
      :body
      (json/read-str :key-fn #(-> % (cstr/replace #"_" "-") keyword))
      :validation-messages))

(defn setup
  []
  (sql.c/migrate-test))

(defn teardown
  []
  (sql.c/teardown))

(defn test-fixture [f]
  (setup)
  (f)
  (teardown))
