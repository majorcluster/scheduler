(ns scheduler.controllers.users
  (:require
   [scheduler.configs :as configs]
   [scheduler.i18n :as i18n]
   [scheduler.logic.tokens :as l.tokens]
   [scheduler.ports.http-out.email :as out.email]
   [scheduler.ports.sql.repositories.users :as r.users])
  (:import
   [java.util Date]))

(defn ^:private current-date []
  (.getTime (Date.)))
(defn create
  [user]
  (let [user-to-create (-> user
                           (assoc :user/email-token
                                  (l.tokens/gen-token-str
                                   (:user/id user)
                                   (current-date)
                                   "email-verification")))
        unique? (-> user-to-create
                    :user/email
                    r.users/find-by-email
                    empty?)]
    (if unique?
      (do (r.users/insert! user-to-create)
          (out.email/send-email
           (:user/email user-to-create)
           (get-in i18n/t [configs/default-lang :verify-email-subject])
           (get-in i18n/t [configs/default-lang :verify-email-body])
           {:name (:user/fname user-to-create)
            :link (str configs/public-url "/users/verify-email/"
                       (:user/email-token user-to-create))})
          (l.tokens/gen-token-str
           (:user/id user)
           (current-date)
           "login"))

      (throw (ex-info "Uniqueness Failure" {:type :duplicated
                                            :message "An user with that email already exists"})))))

(defn verify-token
  [token]
  (let [validity (l.tokens/verify token (current-date))
        user (if (:user-id validity)
               (r.users/find-by-id (:user-id validity))
               nil)]
    (if (and (:valid validity)
             user)
      validity
      {:valid false})))

(defn verify-token-n-user
  [token]
  (let [validity (l.tokens/verify token (current-date))
        user (if (:user-id validity)
               (r.users/find-by-id (:user-id validity))
               nil)]
    (if (and (:valid validity)
             user
             (:user/email-verified user))
      validity
      {:valid false})))

(defn verify-email
  [user-id email-token]
  (let [user (r.users/find-by-id user-id)
        matching-token? (and user
                             (= email-token (:user/email-token user)))
        expired? (-> email-token verify-token :valid not)]
    (cond expired?
          (throw (ex-info "Unauthorized"
                          {:type :unauthorized
                           :message "Unauthorized"}))
          matching-token?
          (r.users/update! (assoc user :user/email-verified true
                                  :user/email-token nil) user-id)
          :else
          (throw (ex-info "Token Mismatch" {:type :token-mismatch
                                            :message "The token does not match the user"})))))

(defn login
  [login-data]
  (let [user (r.users/find-by-email (:email login-data))]
    (cond (= (:user/password user) (:password login-data))
          (l.tokens/gen-token-str
           (:user/id user)
           (current-date)
           "login")
          :else (throw (ex-info "Unauthorized"
                                {:type :unauthorized
                                 :message "Unauthorized"})))))

(defn recover-password
  [email])
