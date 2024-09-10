(ns scheduler.adapters.users-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [scheduler.adapters.users :as a.users]))

(deftest test-sign-up->internal-test
  (testing "sign-up->internal"
    (with-redefs-fn {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                     #'inst-ms     (fn [_] 1234567890123)}
      #(is (= #:user{:password "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
                     :fname "John"
                     :created-at 1234567890123
                     :role "user"
                     :id #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"}
              (a.users/sign-up->internal {:password "password" :fname "John"}))))))

(deftest internal->sql-wire-test
  (testing "internal->sql-wire"
    (is (= {:password "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
            :fname "John"
            :created_at #inst "2009-02-13T23:31:30.123000000-00:00"
            :role "user"
            :id #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"}
           (a.users/internal->sql-wire #:user{:password "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
                                              :fname "John"
                                              :created-at 1234567890123
                                              :role "user"
                                              :id #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"})))))

(deftest sql-wire->internal-test
  (testing "sql-wire->internal"
    (is (= #:user{:password "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
                  :fname "John"
                  :created-at 1234567890123
                  :role "user"
                  :id #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"}
           (a.users/sql-wire->internal #:USERS{:PASSWORD "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
                                               :FNAME "John"
                                               :CREATED_AT 1234567890123
                                               :ROLE "user"
                                               :ID #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"})))))

(deftest admin-sign-up->internal-test
  (testing "sign-up->internal"
    (with-redefs-fn {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                     #'inst-ms     (fn [_] 1234567890123)}
      #(is (= #:user{:password "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
                     :fname "John"
                     :created-at 1234567890123
                     :role "admin"
                     :id #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"
                     :email-verified true}
              (a.users/admin-sign-up->internal {:password "password" :fname "John"}))))))
