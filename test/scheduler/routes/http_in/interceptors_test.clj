(ns scheduler.routes.http-in.interceptors-test
  (:require
   [clj-time.coerce :as timec]
   [clj-time.core :as time]
   [clojure.test :refer :all]
   [scheduler.controllers.dates :as c.dates]
   [scheduler.logic.tokens :as l.tokens]
   [scheduler.ports.http-in.routes.interceptors :as interceptors]
   [scheduler.ports.sql.repositories.users :as r.users])
  (:import
   [clojure.lang ExceptionInfo]))

(def user-id #uuid "f8deaa87-906f-4f5e-b1d3-6919336c3c66")
(def issue-date (time/date-time 2018 1 1 14 0 0))
(def user-token
  (l.tokens/gen-token-str user-id (timec/to-long issue-date) "login"))

(deftest authz-user-test
  (testing "authz-user interceptor should throw an exception"
    (is (thrown-with-msg? ExceptionInfo #"Unauthorized"
                          ((:enter interceptors/authz-user) {:request {:headers {"x-token" "invalid-token"}}})))
    (is (thrown-with-msg? ExceptionInfo #"Unauthorized"
                          ((:enter interceptors/authz-user) {:request {:headers {"x-token" ""}}})))
    (is (thrown-with-msg? ExceptionInfo #"Unauthorized"
                          ((:enter interceptors/authz-user) {:request {:headers {"x-token" nil}}})))
    (is (thrown-with-msg? ExceptionInfo #"Unauthorized"
                          ((:enter interceptors/authz-user) {:request {:headers {}}})))
    (is (thrown-with-msg? ExceptionInfo #"Unauthorized"
                          (with-redefs-fn {#'c.dates/current-date (fn [] (timec/to-long (time/date-time 2018 1 1 15 0 0)))}
                            #((:enter interceptors/authz-user) {:request {:headers {"x-token" user-token}}}))))
    (is (thrown-with-msg? ExceptionInfo #"Unauthorized"
                          (with-redefs-fn {#'c.dates/current-date (fn [] (timec/to-long (time/date-time 2018 1 1 14 0 0)))
                                           #'r.users/find-by-id   (fn [_] {:user/id user-id
                                                                           :user/email-verified false})}
                            #((:enter interceptors/authz-user) {:request {:headers {"x-token" user-token}}}))))
    (is (thrown-with-msg? ExceptionInfo #"Unauthorized"
                          (with-redefs-fn {#'c.dates/current-date (fn [] (timec/to-long (time/date-time 2018 1 1 14 0 0)))
                                           #'r.users/find-by-id   (fn [_] nil)}
                            #((:enter interceptors/authz-user) {:request {:headers {"x-token" user-token}}})))))
  (testing "authz-user interceptor should let the request go through"
    (is (= {:valid true, :user-id #uuid "f8deaa87-906f-4f5e-b1d3-6919336c3c66", :role :user}
           (-> (with-redefs-fn {#'c.dates/current-date (fn [] (timec/to-long (time/date-time 2018 1 1 14 0 0)))
                                #'r.users/find-by-id   (fn [_] {:user/id user-id
                                                                :user/email-verified true})}
                 #((:enter interceptors/authz-user) {:request {:headers {"x-token" user-token}}}))
               :request :authz-user)))
    (is (= {:valid true, :user-id nil, :role :user}
           (-> ((:enter interceptors/authz-user) {:request {:headers {"x-token" "dobry den"}}})
               :request :authz-user)))
    (is (= {:valid true, :user-id nil, :role :admin}
           (-> ((:enter interceptors/authz-user) {:request {:headers {"x-token" "dobrou noc"}}})
               :request :authz-user)))))
