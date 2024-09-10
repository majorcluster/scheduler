(ns scheduler.ports.http-in.routes.users
  (:require
   [pedestal-api-helper.params-helper :as ph]
   [scheduler.adapters.users :as a.users]
   [scheduler.controllers.users :as c.users]
   [scheduler.ports.http-in.routes.interceptors :as i]))

(defn signup
  [request]
  (let [token (-> (get request :json-params {})
                  (ph/validate-and-mop!!
                   {"fname"        [{:validate/type :validate/mandatory}
                                    {:validate/type :validate/min, :validate/value 2}
                                    {:validate/type :validate/max, :validate/value 100}]
                    "lname"        [{:validate/type :validate/mandatory}
                                    {:validate/type :validate/min, :validate/value 2}
                                    {:validate/type :validate/max, :validate/value 100}]
                    "phone"        [{:validate/type :validate/mandatory}
                                    {:validate/type :validate/min, :validate/value 3}
                                    {:validate/type :validate/max, :validate/value 32}]
                    "email"        [{:validate/type :validate/mandatory}
                                    {:validate/type :validate/min, :validate/value 5}
                                    {:validate/type :validate/max, :validate/value 100}]
                    "password"     [{:validate/type :validate/mandatory}
                                    {:validate/type :validate/min, :validate/value 8}
                                    {:validate/type :validate/max, :validate/value 100}]}
                   ["fname","lname","phone","email","password"])
                  a.users/sign-up->internal
                  c.users/create)]
    {:status 204
     :headers {"x-token" token}}))

(defn verify-email
  [request]
  (let [email-token (get-in request [:path-params :token])
        user-id (get-in request [:authz-user :user-id])
        _ (c.users/verify-email user-id email-token)]
    {:status 200 :body {:message "Email verified"}}))

(defn login
  [request]
  (let [token (-> (get request :json-params {})
                  (ph/validate-and-mop!!
                   {"email"        [{:validate/type :validate/mandatory}
                                    {:validate/type :validate/min, :validate/value 4}
                                    {:validate/type :validate/max, :validate/value 100}]
                    "password"     [{:validate/type :validate/mandatory}
                                    {:validate/type :validate/min, :validate/value 3}
                                    {:validate/type :validate/max, :validate/value 100}]}
                   ["email", "password"])
                  a.users/login->internal
                  c.users/login)]
    {:status 200
     :headers {"x-token" token}}))

(defn recover-password
  [request]
  (let [_ (-> (get request :json-params {})
              (ph/validate-and-mop!!
               {"email"        [{:validate/type :validate/mandatory}
                                {:validate/type :validate/min, :validate/value 5}
                                {:validate/type :validate/max, :validate/value 100}]}
               ["email"])
              :email
              c.users/recover-password)]
    {:status 200 :body {:message "Recovery E-mail has been sent"}}))

(defn change-password
  [request]
  (let [token (get-in request [:headers "x-token"] "")
        user-id (get-in request [:authz-user :user-id])
        _ (-> (get request :json-params {})
              (ph/validate-and-mop!!
               {"new-password"    [{:validate/type :validate/mandatory}
                                   {:validate/type :validate/min, :validate/value 8}
                                   {:validate/type :validate/max, :validate/value 100}]}
               ["new-password"])
              :new-password
              (#(c.users/change-password user-id token
                                         (a.users/change-password->internal %))))]
    {:status 200 :body {:message "Password changed"}}))

(defn admin-signup-email
  [request]
  (let [_ (-> (get request :json-params {})
              (ph/validate-and-mop!!
               {"email"        [{:validate/type :validate/mandatory}
                                {:validate/type :validate/min, :validate/value 5}
                                {:validate/type :validate/max, :validate/value 100}]}
               ["email"])
              :email
              c.users/admin-signup-email)]
    {:status 200 :body {:message "Admin signup e-mail has been sent"}}))

(defn admin-signup
  [request]
  (let [validity (get request :authz-user {:valid false
                                           :email ""})
        _ (-> (get request :json-params {})
              (ph/validate-and-mop!!
               {"fname"        [{:validate/type :validate/mandatory}
                                {:validate/type :validate/min, :validate/value 2}
                                {:validate/type :validate/max, :validate/value 100}]
                "lname"        [{:validate/type :validate/mandatory}
                                {:validate/type :validate/min, :validate/value 2}
                                {:validate/type :validate/max, :validate/value 100}]
                "phone"        [{:validate/type :validate/mandatory}
                                {:validate/type :validate/min, :validate/value 3}
                                {:validate/type :validate/max, :validate/value 32}]
                "email"        [{:validate/type :validate/mandatory}
                                {:validate/type :validate/min, :validate/value 5}
                                {:validate/type :validate/max, :validate/value 100}]
                "password"     [{:validate/type :validate/mandatory}
                                {:validate/type :validate/min, :validate/value 8}
                                {:validate/type :validate/max, :validate/value 100}]}
               ["fname","lname","phone","email","password"])
              a.users/admin-sign-up->internal
              (c.users/create-verified (:email validity)))]
    {:status 204}))

(def specs #{["/users/signup" :post (conj i/json-public-interceptors `signup)]
             ["/users/verify-email/:token" :get (conj i/json-gateway-interceptors `verify-email)]
             ["/users/login" :post (conj i/json-public-interceptors `login)]
             ["/users/recover-password" :post (conj i/json-public-interceptors `recover-password)]
             ["/users/change-password" :post (conj i/json-gateway-interceptors `change-password)]
             ["/users/admin-signup-email" :post (conj i/json-admin-interceptors `admin-signup-email)]
             ["/users/admin-signup" :post (conj i/json-email-gateway-interceptors `admin-signup)]})
