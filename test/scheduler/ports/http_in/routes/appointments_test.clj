(ns scheduler.ports.http-in.routes.appointments-test
  (:require
   [clj-time.coerce :as coerce]
   [clj-time.core :as time]
   [clojure.data.json :as cjson]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [core-test :refer [extract-validation-msgs login service test-fixture
                      user+login]]
   [io.pedestal.test :refer [response-for]]
   [scheduler.controllers.dates :as c.dates]
   [scheduler.ports.sql.repositories.appointments :as r.appointments]))

(use-fixtures :each test-fixture)

(def root-auth-headers {"Content-Type" "application/json"
                        "X-TOKEN" "dobry den"})
(def admin-auth-headers {"Content-Type" "application/json"
                         "X-TOKEN" "dobrou noc"})
(def json-headers {"Content-Type" "application/json"})

(def appointment
  {:name "my-schedulable"
   :datetime-ranges "50m:1-00:all-days,not-dec24-dec31"})

#_(deftest post-test
  (testing "when schedulable is invalid"
    (let [current-ms (time/date-time 2023 6 6 12 0 0)
          token (user+login current-ms)
          resp (with-redefs-fn
                {#'random-uuid                 (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                 #'inst-ms                     (fn [_] (coerce/to-long current-ms))
                 #'c.dates/current-date (fn [] (coerce/to-long current-ms))}
                 #(response-for service
                                :post "/schedulables"
                                :headers (assoc json-headers
                                           "x-token" token)
                                :body "{}"))]
      (is (= 400 (:status resp)))
      (is (= '("name" "name" "name"
               "datetime-ranges"
               "datetime-ranges"
               "datetime-ranges")
             (map :field (extract-validation-msgs resp))))))
  (testing "when datetime is invalid"
    (let [current-ms (time/date-time 2023 6 6 12 0 0)
          token (login current-ms)
          resp (with-redefs-fn
                {#'random-uuid                 (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                 #'inst-ms                     (fn [_] (coerce/to-long current-ms))
                 #'c.dates/current-date (fn [] (coerce/to-long current-ms))}
                 #(response-for service
                                :post "/schedulables"
                                :headers (assoc json-headers
                                           "x-token" token)
                                :body (cjson/write-str (assoc schedulable
                                                         :datetime-ranges "anything"))))]
      (is (= 400 (:status resp)))
      (is (extract-validation-msgs resp) "")))
  (testing "when datetime ranges overlap"
    (let [current-ms (time/date-time 2023 6 6 12 0 0)
          token (login current-ms)
          resp (with-redefs-fn
                {#'random-uuid                 (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                 #'inst-ms                     (fn [_] (coerce/to-long current-ms))
                 #'c.dates/current-date (fn [] (coerce/to-long current-ms))}
                 #(response-for service
                                :post "/schedulables"
                                :headers (assoc json-headers
                                           "x-token" token)
                                :body (cjson/write-str (assoc schedulable
                                                         :datetime-ranges "60m:8-20:mon-fri,50m:8-18:mon-tue"))))]
      (is (= 400 (:status resp)))
      (is (extract-validation-msgs resp) "")))
  (testing "when unlogged"
    (let [current-ms (time/date-time 2023 6 6 12 0 0)
          resp (with-redefs-fn
                {#'random-uuid                 (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                 #'inst-ms                     (fn [_] (coerce/to-long current-ms))
                 #'c.dates/current-date (fn [] (coerce/to-long current-ms))}
                 #(response-for service
                                :post "/schedulables"
                                :headers json-headers
                                :body "{}"))]
      (is (= 401 (:status resp)))
      (is (= {:type "unauthorized", :message "Unauthorized", :reason {}} (-> resp :body (cjson/read-str :key-fn keyword))))))
  (testing "when successful"
    (let [current-ms (time/date-time 2023 6 6 12 0 0)
          token (login current-ms)
          resp (with-redefs-fn
                {#'random-uuid                 (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                 #'inst-ms                     (fn [_] (coerce/to-long current-ms))
                 #'c.dates/current-date        (fn [] (coerce/to-long current-ms))}
                 #(response-for service
                                :post "/schedulables"
                                :headers (assoc json-headers
                                           "x-token" token)
                                :body (cjson/write-str schedulable)))
          db-schedulable (r.schedulables/find-by-name (:name schedulable))]
      (is (= 204 (:status resp)))
      (is (:name schedulable) (:schedulable/name db-schedulable))))
  (testing "when not unique"
    (let [current-ms (time/date-time 2023 6 6 12 0 0)
          token (login current-ms)
          resp (with-redefs-fn
                {#'random-uuid                 (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                 #'inst-ms                     (fn [_] (coerce/to-long current-ms))
                 #'c.dates/current-date (fn [] (coerce/to-long current-ms))}
                 #(response-for service
                                :post "/schedulables"
                                :headers (assoc json-headers
                                           "x-token" token)
                                :body (cjson/write-str schedulable)))]
      (is (= 400 (:status resp)))
      (is (= {:type "duplicated", :message "A schedulable with that name already exists"} (-> resp :body (cjson/read-str :key-fn keyword)))))))
