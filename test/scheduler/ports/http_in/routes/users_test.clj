(ns scheduler.ports.http-in.routes.users-test
  (:require
   [clj-time.coerce :as coerce]
   [clj-time.core :as time]
   [clojure.data.json :as cjson]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [core-test :refer [extract-validation-msgs service test-fixture]]
   [io.pedestal.test :refer [response-for]]
   [matcher-combinators.test]
   [scheduler.controllers.users :as c.users]
   [scheduler.logic.tokens :as l.tokens]
   [scheduler.ports.http-out.email :as out.email]
   [scheduler.ports.sql.repositories.users :as r.users]))

(use-fixtures :each test-fixture)

(def root-auth-headers {"Content-Type" "application/json"
                        "X-TOKEN" "dobry den"})
(def admin-auth-headers {"Content-Type" "application/json"
                         "X-TOKEN" "dobrou noc"})
(def json-headers {"Content-Type" "application/json"})

(deftest signup-test
  (let [user {:fname "Vladmir"
              :lname "Lenin"
              :phone "123456789"
              :email "lenin@cccp.co"
              :password "12345678"}]
    (testing "no mandatory fields"
      (let [resp (response-for service
                               :post "/users/signup"
                               :headers json-headers
                               :body "{}")]
        (is (= 400
               (:status resp)))
        (is (= '("fname" "fname" "fname" "lname" "lname" "lname" "phone" "phone" "phone" "email" "email" "email" "password" "password" "password")
               (map :field (extract-validation-msgs resp))))))
    (testing "user inserted"
      (let [resp (with-redefs-fn
                   {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                    #'inst-ms     (fn [_] 1234567890123)
                    #'c.users/current-date (fn [] 1234567890123)
                    #'out.email/send-email (fn [_ _ _ _] nil)}
                   #(response-for service
                                  :post "/users/signup"
                                  :headers json-headers
                                  :body (cjson/write-str user)))
            db-user (r.users/find-by-email (:email user))]
        (is (= 204
               (:status resp)))
        (is (= "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyLWlkIjoiMzc0NWEzNjMtY2E3MC00YTQzLTlmNWYtZWYwY2JmZGFiN2U0IiwiaWF0IjoxMjM0NTY3ODkwLCJleHAiOjEyMzQ1NzE0OTAsInR5cGUiOiJsb2dpbiIsImp0aSI6IjM3NDVhMzYzLWNhNzAtNGE0My05ZjVmLWVmMGNiZmRhYjdlNCJ9.G7Mn40ufOFCf7gPNmkRqA0XareOVJn39uDjZVTA3-yI"
               (get-in resp [:headers "x-token"])))
        (is (match?
             db-user
             #:user{:fname "Vladmir"
                    :role "user"
                    :email-verified false
                    :external-type nil
                    :email-token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyLWlkIjoiMzc0NWEzNjMtY2E3MC00YTQzLTlmNWYtZWYwY2JmZGFiN2U0IiwiaWF0IjoxMjM0NTY3ODkwLCJleHAiOjEyMzQ1NzE0OTAsInR5cGUiOiJlbWFpbC12ZXJpZmljYXRpb24iLCJqdGkiOiIzNzQ1YTM2My1jYTcwLTRhNDMtOWY1Zi1lZjBjYmZkYWI3ZTQifQ.y2ZVhQc2QSctpWjvrFrXxzzgF5xaXpyO39eIfsTbVtg"
                    :id #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4"
                    :password "ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f"
                    :password-recovering false
                    :created-at 1234567890123
                    :phone "123456789"
                    :external-token nil
                    :email "lenin@cccp.co"
                    :lname "Lenin"}))))
    (testing "user duplicated not inserted"
      (let [resp (with-redefs-fn
                   {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                    #'inst-ms     (fn [_] 1234567890123)
                    #'c.users/current-date (fn [] 1234567890123)
                    #'out.email/send-email (fn [_ _ _ _] nil)}
                   #(response-for service
                                  :post "/users/signup"
                                  :headers json-headers
                                  :body (cjson/write-str user)))]
        (is (= 400
               (:status resp)))
        (is (= {"type" "duplicated", "message" "An user with that email already exists"}
               (-> (:body resp)
                   (cjson/read-str))))))))

(deftest verify-email-test
  (let [issue-date (time/date-time 2023 6 6 12 0 0)
        email-token (l.tokens/gen-token-str
                     #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7"
                     (coerce/to-long issue-date)
                     "email-verification")
        user {:user/id #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7"
              :user/email-token email-token
              :user/email-verified false
              :user/created-at (coerce/to-long issue-date)
              :user/role "user"
              :user/password-recovering false
              :user/fname "Vladmir"
              :user/lname "Lenin"
              :user/phone "123456789"
              :user/email "lenin@cccp.co"
              :user/password "ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f"}
        _ (r.users/insert! user)]
    (testing "email not verified due to expired login"
      (let [current-ms (time/date-time 2023 6 6 13 0 0)
            login-token (l.tokens/gen-token-str
                         #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7"
                         (coerce/to-long issue-date)
                         "login")
            resp (with-redefs-fn
                   {#'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :get (str "/users/verify-email/" email-token)
                                  :headers (assoc json-headers
                                                  "x-token" login-token)))
            db-user (r.users/find-by-id (:user/id user))]
        (is (= 401 (:status resp)))
        (is (= {:type "unauthorized", :message "Unauthorized", :reason {}} (-> resp :body (cjson/read-str :key-fn keyword))))
        (is (not (:user/email-verified db-user)))))
    (testing "email not verified due to stolen token"
      (let [current-ms (time/date-time 2023 6 6 13 0 0)
            login-token (l.tokens/gen-token-str
                         (random-uuid)
                         (coerce/to-long issue-date)
                         "login")
            resp (with-redefs-fn
                   {#'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :get (str "/users/verify-email/" email-token)
                                  :headers (assoc json-headers
                                                  "x-token" login-token)))
            db-user (r.users/find-by-id (:user/id user))]
        (is (= 401 (:status resp)))
        (is (= {:type "unauthorized", :message "Unauthorized", :reason {}} (-> resp :body (cjson/read-str :key-fn keyword))))
        (is (not (:user/email-verified db-user)))))
    (testing "email not verified due missing login token"
      (let [current-ms (time/date-time 2023 6 6 13 0 0)
            resp (with-redefs-fn
                   {#'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :get (str "/users/verify-email/" email-token)
                                  :headers json-headers))
            db-user (r.users/find-by-id (:user/id user))]
        (is (= 401 (:status resp)))
        (is (= {:type "unauthorized", :message "Unauthorized", :reason {}} (-> resp :body (cjson/read-str :key-fn keyword))))
        (is (not (:user/email-verified db-user)))))
    (testing "email not verified due to expired email token"
      (let [current-ms (time/date-time 2023 6 6 13 0 0)
            login-token (l.tokens/gen-token-str
                         #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7"
                         current-ms
                         "login")
            resp (with-redefs-fn
                   {#'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :get (str "/users/verify-email/" email-token)
                                  :headers (assoc json-headers
                                                  "x-token" login-token)))
            db-user (r.users/find-by-id (:user/id user))]
        (is (= 401 (:status resp)))
        (is (= {:type "unauthorized", :message "Unauthorized"} (-> resp :body (cjson/read-str :key-fn keyword))))
        (is (not (:user/email-verified db-user)))))
    (testing "email verified"
      (let [login-token (l.tokens/gen-token-str
                         #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7"
                         (coerce/to-long issue-date)
                         "login")
            resp (with-redefs-fn
                   {#'inst-ms (fn [_] (coerce/to-long issue-date))
                    #'c.users/current-date (fn [] (coerce/to-long issue-date))}
                   #(response-for service
                                  :get (str "/users/verify-email/" email-token)
                                  :headers (assoc json-headers
                                                  "x-token" login-token)))
            db-user (r.users/find-by-id (:user/id user))]
        (is (= 200 (:status resp)))
        (is (= {:message "Email verified"} (-> resp :body (cjson/read-str :key-fn keyword))))
        (is (:user/email-verified db-user))
        (is (nil? (:user/email-token db-user)))))))

(deftest login-test
  (let [issue-date (time/date-time 2023 6 6 12 0 0)
        user {:user/id #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7"
              :user/email-token nil
              :user/email-verified true
              :user/created-at (coerce/to-long issue-date)
              :user/role "user"
              :user/password-recovering false
              :user/fname "Vladmir"
              :user/lname "Lenin"
              :user/phone "123456789"
              :user/email "lenin@cccp.co"
              :user/password "ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f"}
        _ (r.users/insert! user)]
    (testing "when password does not match"
      (let [current-ms (time/date-time 2023 6 6 13 0 0)
            resp (with-redefs-fn
                   {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                    #'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :post (str "/users/login")
                                  :headers json-headers
                                  :body (cjson/write-str {:email "lenin@cccp.co" :password "11111111"})))]
        (is (= 401 (:status resp)))
        (is (= {:type "unauthorized", :message "Unauthorized"} (-> resp :body (cjson/read-str :key-fn keyword))))))
    (testing "when user is not found"
      (let [current-ms (time/date-time 2023 6 6 13 0 0)
            resp (with-redefs-fn
                   {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                    #'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :post (str "/users/login")
                                  :headers json-headers
                                  :body (cjson/write-str {:email "kolontai@cccp.co" :password "12345678"})))]
        (is (= 401 (:status resp)))
        (is (= {:type "unauthorized", :message "Unauthorized"} (-> resp :body (cjson/read-str :key-fn keyword))))))
    (testing "when mandatory data is not sent"
      (let [_ (r.users/update! (assoc user :user/email-verified false) (:user/id user))
            current-ms (time/date-time 2023 6 6 13 0 0)
            resp (with-redefs-fn
                   {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                    #'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :post (str "/users/login")
                                  :headers json-headers
                                  :body "{}"))]
        (is (= 400 (:status resp)))
        (is (= '("email" "email" "email" "password" "password" "password")
               (map :field (extract-validation-msgs resp))))))
    (testing "when login succeeds"
      (let [_ (r.users/update! (assoc user :user/email-verified true) (:user/id user))
            current-ms (time/date-time 2023 6 6 13 0 0)
            resp (with-redefs-fn
                   {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                    #'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :post (str "/users/login")
                                  :headers json-headers
                                  :body (cjson/write-str {:email "lenin@cccp.co" :password "12345678"})))]
        (is (= 200 (:status resp)))
        (is (= "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyLWlkIjoiMzc0NWEzNjMtY2E3MC00YTQzLTlmNWYtMDBlZjBjYmZkYWI3IiwiaWF0IjoxNjg2MDU2NDAwLCJleHAiOjE2ODYwNjAwMDAsInR5cGUiOiJsb2dpbiIsImp0aSI6IjM3NDVhMzYzLWNhNzAtNGE0My05ZjVmLWVmMGNiZmRhYjdlNCJ9.rGbpciXqTgJxZD-e4X6I4jnMkcv56g4IqARUVdAse38"
               (get-in resp [:headers "x-token"])))))))

(deftest recover-password-test
  (let [issue-date (time/date-time 2023 6 6 12 0 0)
        user {:user/id #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7"
              :user/email-token nil
              :user/email-verified true
              :user/created-at (coerce/to-long issue-date)
              :user/role "user"
              :user/password-recovering false
              :user/fname "Vladmir"
              :user/lname "Lenin"
              :user/phone "123456789"
              :user/email "lenin@cccp.co"
              :user/password "ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f"}
        _ (r.users/insert! user)]
    (testing "when mandatory data is not sent"
      (let [resp (with-redefs-fn
                   {}
                   #(response-for service
                                  :post (str "/users/recover-password")
                                  :headers json-headers
                                  :body "{}"))]
        (is (= 400 (:status resp)))
        (is (= '("email" "email" "email")
               (map :field (extract-validation-msgs resp))))))
    (testing "user does not exist"
      (let [current-ms (time/date-time 2023 6 6 12 0 0)
            resp (with-redefs-fn
                   {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                    #'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :post (str "/users/recover-password")
                                  :headers json-headers
                                  :body (cjson/write-str {:email "test@test.com"})))]
        (is (= 200 (:status resp)))))
    (testing "user exists"
      (let [current-ms (time/date-time 2023 6 6 12 0 0)
            resp (with-redefs-fn
                   {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                    #'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))
                    #'out.email/send-email (fn [_ _ _ _] nil)}
                   #(response-for service
                                  :post (str "/users/recover-password")
                                  :headers json-headers
                                  :body (cjson/write-str {:email "lenin@cccp.co"})))
            db-user (r.users/find-by-id (:user/id user))]
        (is (= 200 (:status resp)))
        (is (:user/password-recovering db-user))
        (is (not (empty? (:user/email-token db-user))))))))

(deftest change-password
  (let [issue-date (time/date-time 2023 6 6 12 0 0)
        email-token (l.tokens/gen-token-str
                     #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7"
                     (coerce/to-long issue-date)
                     "email-verification")
        user {:user/id #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7"
              :user/email-token email-token
              :user/email-verified true
              :user/created-at (coerce/to-long issue-date)
              :user/role "user"
              :user/password-recovering true
              :user/fname "Vladmir"
              :user/lname "Lenin"
              :user/phone "123456789"
              :user/email "lenin@cccp.co"
              :user/password "ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f"}
        _ (r.users/insert! user)]
    (testing "when token is malformed"
      (let [current-ms (time/date-time 2023 6 6 12 0 0)
            resp (with-redefs-fn
                   {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                    #'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :post "/users/change-password"
                                  :headers (assoc json-headers
                                                  "x-token" "anything")
                                  :body "{}"))]
        (is (= 401 (:status resp)))
        (is (= {:type "unauthorized", :message "Unauthorized", :reason {}} (-> resp :body (cjson/read-str :key-fn keyword))))))
    (testing "when token is outdated"
      (let [current-ms (time/date-time 2023 6 6 13 0 0)
            resp (with-redefs-fn
                   {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                    #'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :post "/users/change-password"
                                  :headers (assoc json-headers
                                                  "x-token" email-token)
                                  :body "{}"))]
        (is (= 401 (:status resp)))
        (is (= {:type "unauthorized", :message "Unauthorized", :reason {}} (-> resp :body (cjson/read-str :key-fn keyword))))))
    (testing "when no password recovering or token is at db"
      (let [current-ms (time/date-time 2023 6 6 12 0 0)
            _ (r.users/update! (assoc user :user/password-recovering false) (:user/id user))
            resp (with-redefs-fn
                   {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                    #'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :post "/users/change-password"
                                  :headers (assoc json-headers
                                                  "x-token" email-token)
                                  :body (cjson/write-str {:new-password "12345678"})))]
        (is (= 401 (:status resp)))
        (is (= {:type "token-mismatch", :message "The token does not match the user"} (-> resp :body (cjson/read-str :key-fn keyword)))))
      (let [current-ms (time/date-time 2023 6 6 12 0 0)
            _ (r.users/update! (assoc user :user/password-recovering true
                                      :user/email-token nil) (:user/id user))
            resp (with-redefs-fn
                   {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                    #'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :post "/users/change-password"
                                  :headers (assoc json-headers
                                                  "x-token" email-token)
                                  :body (cjson/write-str {:new-password "12345678"})))]
        (is (= 401 (:status resp)))
        (is (= {:type "token-mismatch", :message "The token does not match the user"} (-> resp :body (cjson/read-str :key-fn keyword))))))
    (testing "password is changed"
      (let [current-ms (time/date-time 2023 6 6 12 59 59)
            _ (r.users/update! (assoc user :user/password-recovering true
                                      :user/email-token email-token) (:user/id user))
            resp (with-redefs-fn
                   {#'random-uuid (fn [] #uuid "3745a363-ca70-4a43-9f5f-ef0cbfdab7e4")
                    #'inst-ms (fn [_] (coerce/to-long current-ms))
                    #'c.users/current-date (fn [] (coerce/to-long current-ms))}
                   #(response-for service
                                  :post "/users/change-password"
                                  :headers (assoc json-headers
                                                  "x-token" email-token)
                                  :body (cjson/write-str {:new-password "00000000"})))
            db-user (r.users/find-by-id (:user/id user))]
        (is (= 200 (:status resp)))
        (is (not (:user/password-recovering db-user)))
        (is (nil? (:user/email-token db-user)))
        (is (= (:user/password db-user) "7e071fd9b023ed8f18458a73613a0834f6220bd5cc50357ba3493c6040a9ea8c"))))))
