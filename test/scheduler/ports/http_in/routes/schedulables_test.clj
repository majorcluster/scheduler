(ns scheduler.ports.http-in.routes.schedulables-test
  (:require
   [clj-time.coerce :as coerce]
   [clj-time.core :as time]
   [clojure.data.json :as cjson]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [core-test :refer [extract-validation-msgs login service test-fixture
                      user+login resp->map]]
   [io.pedestal.test :refer [response-for]]
   [scheduler.controllers.dates :as c.dates]
   [scheduler.ports.sql.repositories.schedulables :as r.schedulables]
   [pedestal-api-helper.params-helper :as ph]))

(use-fixtures :each test-fixture)

(def root-auth-headers {"Content-Type" "application/json"
                        "X-TOKEN" "dobry den"})
(def admin-auth-headers {"Content-Type" "application/json"
                         "X-TOKEN" "dobrou noc"})
(def json-headers {"Content-Type" "application/json"})

(def schedulable
  {:name "my-schedulable"
   :datetime-ranges "50m:1-00:all-days,not-dec24-dec31"})

(defn insert-schedulable
  ([schedulable token-fn id]
    (let [current-ms (time/date-time 2023 6 6 12 0 0)
          token (token-fn current-ms)
          resp (with-redefs-fn
                {#'random-uuid                 (fn []  id)
                 #'inst-ms                     (fn [_] (coerce/to-long current-ms))
                 #'c.dates/current-date        (fn []  (coerce/to-long current-ms))}
                 #(response-for service
                                :post "/schedulables"
                                :headers (assoc json-headers
                                           "x-token" token)
                                :body (cjson/write-str schedulable)))
          db-schedulable (r.schedulables/find-by-name (:name schedulable))]
      [resp db-schedulable]))
  ([schedulable]
   (insert-schedulable schedulable login #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"))
  ([schedulable token-fn]
   (insert-schedulable schedulable token-fn #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")))

(defn update-schedulable
  [partial-schedulable]
  (let [current-ms (time/date-time 2023 6 6 12 0 0)
        token (login current-ms)
        resp (with-redefs-fn
              {#'inst-ms                     (fn [_] (coerce/to-long current-ms))
               #'c.dates/current-date        (fn []  (coerce/to-long current-ms))}
               #(response-for service
                              :patch "/schedulables"
                              :headers (assoc json-headers
                                         "x-token" token)
                              :body (cjson/write-str partial-schedulable)))
        db-schedulable (if (ph/is-uuid (:id partial-schedulable))
                        (r.schedulables/find-by-id!
                          (:id partial-schedulable))
                        nil)]
    [resp db-schedulable]))

(defn get-schedulables
  []
  (let [current-ms (time/date-time 2023 6 6 12 0 0)
        token (login current-ms)
        resp (with-redefs-fn
              {#'random-uuid                 (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
               #'inst-ms                     (fn [_] (coerce/to-long current-ms))
               #'c.dates/current-date        (fn [] (coerce/to-long current-ms))}
               #(response-for service
                              :get "/schedulables"
                              :headers (assoc json-headers
                                         "x-token" token)))]
    resp))

(defn by-id
  [id]
  (let [current-ms (time/date-time 2023 6 6 12 0 0)
        token (login current-ms)
        resp (with-redefs-fn
              {#'random-uuid                 (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
               #'inst-ms                     (fn [_] (coerce/to-long current-ms))
               #'c.dates/current-date        (fn [] (coerce/to-long current-ms))}
               #(response-for service
                              :get (str "/schedulables/" id)
                              :headers (assoc json-headers
                                         "x-token" token)))]
    resp))

(deftest schedulable-crud-test
  (testing "[POST]/schedulables when schedulable is invalid"
    (let [[resp _] (insert-schedulable {} user+login)]
      (is (= 400 (:status resp)))
      (is (= '("name" "name" "name"
                      "datetime-ranges"
                      "datetime-ranges"
                      "datetime-ranges")
             (map :field (extract-validation-msgs resp))))))
  (testing "[POST]/schedulables when datetime is invalid"
    (let [[resp _] (insert-schedulable
                    (assoc schedulable
                           :datetime-ranges "anything"))]
      (is (= 400 (:status resp)))
      (is (= '("Field datetime-ranges is not valid") (map :message (extract-validation-msgs resp))))))
  (testing "[POST]/schedulables when datetime ranges overlap"
    (let [[resp _] (insert-schedulable
                    (assoc schedulable
                      :datetime-ranges "60m:8-20:mon-fri,50m:8-18:mon-tue"))]
      (is (= 400 (:status resp)))
      (is (= '("Field datetime-ranges is not valid") (map :message (extract-validation-msgs resp))))))
  (testing "[POST]/schedulables when unlogged"
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
  (testing "[POST]/schedulables when successful"
    (let [[resp db-schedulable] (insert-schedulable schedulable)]
      (is (= 204 (:status resp)))
      (is (:name schedulable) (:schedulable/name db-schedulable))))
  (testing "[POST]/schedulables when not unique"
    (let [[resp _] (insert-schedulable schedulable)]
      (is (= 400 (:status resp)))
      (is (= {:type "duplicated", :message "A schedulable with that name already exists"} (-> resp :body (cjson/read-str :key-fn keyword))))))

  (testing "[GET]/schedulables when there are schedulables"
    (let [_ (insert-schedulable (assoc schedulable
                                  :name "my-second-schedulable")
                                login
                                #uuid "9b28e07b-ded2-4df7-b201-c33b822e40db")
          resp (get-schedulables)]
      (is (= 200 (:status resp)))
      (is (= {:schedulables [{:id "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"
                              :name "my-schedulable"
                              :description nil :created_at 1686052800000
                              :datetime_ranges "50m:1-00:all-days,not-dec24-dec31"
                              :closed nil}
                             {:id "9b28e07b-ded2-4df7-b201-c33b822e40db"
                              :name "my-second-schedulable", :description nil
                              :created_at 1686052800000 :datetime_ranges "50m:1-00:all-days,not-dec24-dec31"
                              :closed nil}]}
             (resp->map resp)))))
  (testing "[GET]/schedulables when there are no schedulables"
    (let [resp (with-redefs-fn
                {#'r.schedulables/find-all (fn [] [])}
                #(get-schedulables))]
      (is (= 200 (:status resp)))
      (is (= {:schedulables []}
             (resp->map resp)))))

  (testing "[PATCH]/schedulables when schedulable id is not sent"
    (let [[resp _] (update-schedulable {})]
      (is (= 400 (:status resp)))
      (is (= '("id" "id")
             (map :field (extract-validation-msgs resp))))))
  (testing "[PATCH]/schedulables when schedulable id is invalid"
    (let [[resp _] (update-schedulable {:id "anything"})]
      (is (= 400 (:status resp)))
      (is (= [{:field "id", :message ":id needs to match uuid v4 pattern"}]
             (extract-validation-msgs resp)))))
  (testing "[PATCH]/schedulables when schedulable is invalid"
    (let [[resp _] (update-schedulable {:id "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"})]
      (is (= 400 (:status resp)))
      (is (=  "{\"type\":\"min-attrs\",\"message\":\"Provide at least one attribute to change\"}" (-> resp :body)))))
  (testing "[PATCH]/schedulables when datetime is invalid"
    (let [[resp _] (update-schedulable
                    {:id "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"
                     :datetime-ranges "anything"})]
      (is (= 400 (:status resp)))
      (is (= '("Field datetime-ranges is not valid") (map :message (extract-validation-msgs resp))))))
  (testing "[PATCH]/schedulables when datetime ranges overlap"
    (let [[resp _] (update-schedulable
                    {:id "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"
                     :datetime-ranges "60m:8-20:mon-fri,50m:8-18:mon-tue"})]
      (is (= 400 (:status resp)))
      (is (= '("Field datetime-ranges is not valid") (map :message (extract-validation-msgs resp))))))
  (testing "[PATCH]/schedulables when schedulable is not found"
    (let [[resp _] (update-schedulable {:id "5745a363-ca70-4a43-9f5f-ef0cbfdab7a2"})]
      (is (= 404 (:status resp)))
      (is (=  "{\"type\":\"not-found\",\"message\":\"A schedulable with that id was not found\"}" (-> resp :body)))))
  (testing "[PATCH]/schedulables when unlogged"
    (let [current-ms (time/date-time 2023 6 6 12 0 0)
          resp (with-redefs-fn
                {#'inst-ms              (fn [_] (coerce/to-long current-ms))
                 #'c.dates/current-date (fn [] (coerce/to-long current-ms))}
                 #(response-for service
                                :patch "/schedulables"
                                :headers json-headers
                                :body (cjson/write-str {:id "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"
                                                        :datetime-ranges "50m:1-00:all-days,not-dec24-dec31"})))]
      (is (= 401 (:status resp)))
      (is (= {:type "unauthorized", :message "Unauthorized", :reason {}} (-> resp :body (cjson/read-str :key-fn keyword))))))
  (testing "[PATCH]/schedulables when successful"
    (let [[resp db-schedulable] (update-schedulable
                                 {:id "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"
                                  :name "updated-name"})]
      (is (= 204 (:status resp)))
      (is (= "" (:body resp)))
      (is (= "updated-name" (:schedulable/name db-schedulable)))))
  (testing "[PATCH]/schedulables when not unique"
    (let [[resp _] (update-schedulable
                    {:id "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"
                     :name "my-second-schedulable"})]
      (is (= 400 (:status resp)))
      (is (= {:type "duplicated", :message "A schedulable with that name already exists"} (-> resp :body (cjson/read-str :key-fn keyword))))))

  (testing "[GET]/schedulables/:id when found"
    (let [resp (by-id #uuid "9b28e07b-ded2-4df7-b201-c33b822e40db")]
      (is (= 200 (:status resp)))
      (is (= {:schedulable {:id "9b28e07b-ded2-4df7-b201-c33b822e40db"
                            :name "my-second-schedulable", :description nil
                            :created_at 1686052800000 :datetime_ranges "50m:1-00:all-days,not-dec24-dec31"
                            :closed nil}}
             (resp->map resp)))))
  (testing "[GET]/schedulables/:id when not found"
    (let [resp (by-id #uuid "5a28e07b-ded2-4df7-b201-c33b822e40cc")]
      (is (= 404 (:status resp)))
      (is (=  "{\"type\":\"not-found\",\"message\":\"A schedulable with that id was not found\"}" (-> resp :body))))))
