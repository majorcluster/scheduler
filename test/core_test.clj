(ns core-test
  (:require
   [clj-data-adapter.core :as data-adapter]
   [clj-time.coerce :as coerce]
   [clj-time.core :as time]
   [clojure.data.json :as cjson]
   [clojure.string :as cstr]
   [clojure.test :refer :all]
   [io.pedestal.http :as bootstrap]
   [io.pedestal.test :refer [response-for]]
   [scheduler.controllers.dates :as c.dates]
   [scheduler.ports.http-in.core :as service]
   [scheduler.ports.sql.core :as sql.c]
   [scheduler.ports.sql.repositories.users :as r.users])
  (:import
   [clojure.lang ExceptionInfo]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))
(def json-headers {"Content-Type" "application/json"})

(defmethod assert-expr 'thrown-with-data? [msg form]
  ;; (is (thrown-with-data? pred expr))
  ;; Asserts that the message string of the ExceptionInfo exception matches
  ;; (with re-find) the regular expression re.
  ;; Also asserts that the attached data matches the given predicate
  (let [re (nth form 1)
        data (nth form 2)
        body (nthnext form 3)]
    `(try ~@body
          (do-report {:type :fail, :message ~msg, :expected '~form, :actual nil})
          (catch ExceptionInfo e#
            (let [m# (.getMessage e#)
                  dta# (ex-data e#)]
              (cond
                (not (re-find ~re m#))
                (do-report {:type :fail, :message ~msg,
                            :expected '~re, :actual m#})
                (not= dta# ~data)
                (do-report {:type :fail, :message ~msg,
                            :expected '~data, :actual dta#})

                :else
                (do-report {:type :pass, :message ~msg,
                            :expected '~re, :actual m#})))
            e#))))

(defn extract-validation-msgs
  [resp]
  (-> resp
      :body
      (cjson/read-str :key-fn #(-> % (cstr/replace #"_" "-") keyword))
      :validation-messages))

(defn print-> [x msg]
  (println msg x)
  x)

(defn user+login
  ([current-ms]
   (let [user {:user/id #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7"
               :user/email-token "lenin@cccp.co"
               :user/email-verified true
               :user/created-at (coerce/to-long current-ms)
               :user/role "user"
               :user/password-recovering true
               :user/fname "Vladmir"
               :user/lname "Lenin"
               :user/phone "123456789"
               :user/email "lenin@cccp.co"
               :user/password "ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f"}
         _ (r.users/insert! user)]
     (-> (with-redefs-fn
           {#'random-uuid          (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
            #'inst-ms              (fn [_] (coerce/to-long current-ms))
            #'c.dates/current-date (fn [] (coerce/to-long current-ms))}
           #(response-for service
                          :post (str "/users/login")
                          :headers json-headers
                          :body (cjson/write-str {:email "lenin@cccp.co" :password "12345678"})))
         (get-in [:headers "x-token"]))))
  ([]
   (user+login (time/date-time 2023 6 6 13 0 0))))

(defn login
  ([current-ms login+password id]
   (-> (with-redefs-fn
         {#'random-uuid          (fn [] id)
          #'inst-ms              (fn [_] (coerce/to-long current-ms))
          #'c.dates/current-date (fn [] (coerce/to-long current-ms))}
         #(response-for service
                        :post (str "/users/login")
                        :headers json-headers
                        :body (cjson/write-str login+password)))
       (get-in [:headers "x-token"])))
  ([current-ms]
   (login current-ms
          {:email "lenin@cccp.co" :password "12345678"}
          #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"))
  ([]
   (login (time/date-time 2023 6 6 13 0 0))))

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

(defn resp->map
  [resp]
  (-> resp :body cjson/read-str (#(data-adapter/transform-keys keyword %))))
