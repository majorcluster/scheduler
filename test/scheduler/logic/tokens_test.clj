(ns scheduler.logic.tokens-test
  (:require
   [clj-jwt.core :as jwtc]
   [clj-time.coerce :refer [from-epoch to-long]]
   [clj-time.core :refer [date-time]]
   [clojure.test :refer :all]
   [scheduler.logic.tokens :as c.tokens]))

(deftest gen-token-test
  (testing  "gen-token should generate a token with the correct claims"
    (let [user-id #uuid "3396bd16-6fdd-41cb-947a-87d600a9ca08"
          timestamp (to-long (date-time 2000 1 1 23 0 0))
          type "email-verification"
          token (c.tokens/gen-token-str user-id timestamp type)
          {:keys [claims]} (-> token jwtc/str->jwt)]
      (is (= (str user-id) (:user-id claims)))
      (is (= type (:type claims)))
      (is (= (date-time 2000 1 1 23 0 0) (from-epoch (:iat claims))))
      (is (= (date-time 2000 1 2 0 0 0) (from-epoch (:exp claims)))))))

(deftest verify-test
  (testing "verify should return a valid token"
    (let [user-id #uuid "3396bd16-6fdd-41cb-947a-87d600a9ca08"
          creation-timestamp (to-long (date-time 2000 1 1 23 0 0))
          type "email-verification"
          token (c.tokens/gen-token-str user-id creation-timestamp type)
          now (to-long (date-time 2000 1 1 23 59 59))
          {:keys [valid user-id]} (c.tokens/verify token now)]
      (is valid)
      (is (= user-id #uuid "3396bd16-6fdd-41cb-947a-87d600a9ca08"))))
  (testing "verify should return an invalid token"
    (let [user-id #uuid "3396bd16-6fdd-41cb-947a-87d600a9ca08"
          creation-timestamp (to-long (date-time 2000 1 1 23 0 0))
          type "email-verification"
          token (c.tokens/gen-token-str user-id creation-timestamp type)
          now (to-long (date-time 2000 1 2 0 0 0))
          {:keys [valid]} (c.tokens/verify token now)]
      (is (not valid)))))
