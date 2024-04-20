(ns scheduler.ports.http-in.routes.users
  (:require
   [pedestal-api-helper.params-helper :as ph]
   [scheduler.adapters.users :as a.users]
   [scheduler.controllers.users :as c.users]))

(defn signup [request]
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

(defn verify-email [request]
  (let [email-token (get-in request [:path-params :token])
        user-id (get-in request [:authz-user :user-id])
        _ (c.users/verify-email user-id email-token)]
    {:status 200 :body {:message "Email verified"}}))

(defn print->
  [m msg]
  (println msg m)
  m)

(defn login [request]
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
