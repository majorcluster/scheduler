(ns scheduler.logic.tokens
  (:require
   [clj-jwt.core :as jwtc]
   [clj-time.coerce :refer [from-epoch from-long]]
   [clj-time.core :refer [hours plus]]
   [scheduler.configs :as configs]))

(defn gen-token
  [user-id timestamp type]
  (let [claims {:user-id user-id
                :iat (-> timestamp from-long)
                :exp (-> timestamp
                         from-long ;1h
                         (plus (hours 1)))
                :type type
                :jti (random-uuid)}]
    (-> claims
        jwtc/jwt
        (jwtc/sign :HS256 configs/token-secret))))

(defn gen-token-str
  [user-id date type]
  (jwtc/to-str (gen-token user-id date type)))

(defn gen-email-token
  [email timestamp type]
  (let [claims {:email email
                :iat (-> timestamp from-long)
                :exp (-> timestamp
                         from-long ;1h
                         (plus (hours 1)))
                :type type
                :jti (random-uuid)}]
    (-> claims
        jwtc/jwt
        (jwtc/sign :HS256 configs/token-secret))))

(defn gen-email-token-str
  [email date type]
  (jwtc/to-str (gen-email-token email date type)))

(defn verify
  [token now]
  (let [decoded (try (-> token jwtc/str->jwt) (catch Exception _ {}))
        {:keys [exp user-id]} (:claims decoded)]
    (if (and exp
             (number? exp)
             (jwtc/verify decoded :HS256 configs/token-secret)
             (> 0 (.compareTo (from-long now) (from-epoch exp))))
      {:valid true
       :user-id (parse-uuid user-id)}
      {:valid false})))

(defn verify-email
  [token now]
  (let [decoded (try (-> token jwtc/str->jwt) (catch Exception _ {}))
        {:keys [exp email]} (:claims decoded)]
    (if (and exp
             (number? exp)
             (jwtc/verify decoded :HS256 configs/token-secret)
             (> 0 (.compareTo (from-long now) (from-epoch exp))))
      {:valid true
       :email email}
      {:valid false})))
