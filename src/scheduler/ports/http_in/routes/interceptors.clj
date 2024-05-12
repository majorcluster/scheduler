(ns scheduler.ports.http-in.routes.interceptors
  (:require
   [clj-data-adapter.core :as data-adapters]
   [clojure.data.json :as cjson]
   [io.pedestal.http :as http]
   [io.pedestal.http.body-params :as body-params]
   [scheduler.configs :as configs]
   [scheduler.controllers.users :as c.users]
   [scheduler.ports.http-in.routes.error-handler :refer [service-error-handler]])
  (:import
   [clojure.lang ExceptionInfo]))

(defn convert-to-json
  [m]
  (cond (map? m) (cjson/write-str m :key-fn data-adapters/kebab-key->snake-str
                                  :escape-unicode false)
        :else m))

(defn json-out
  []
  {:name ::json-out
   :leave (fn [context]
            (->> (:response context)
                 :body
                 convert-to-json
                 (assoc-in context [:response :body])))})

(defn authorization-error
  "Throws an authorization error"
  ([]
   (authorization-error "Unauthorized" {}))
  ([message]
   (authorization-error message {}))
  ([message data]
   (throw (ex-info "Unauthorized"
                   {:type :unauthorized
                    :message message
                    :reason data}))))

(defn authz
  [token-authz-fn]
  (fn [context]
    (try
      (let [token (get-in context [:request :headers "x-token"] "")
            validity (cond (= token configs/user-passphrase) {:valid true
                                                              :user-id nil
                                                              :role :user}
                           (= token configs/admin-passphrase) {:valid true
                                                               :user-id nil
                                                               :role :admin}
                           :else (-> token
                                     token-authz-fn
                                     (assoc :role :user)))]
        (if (:valid validity)
          (assoc-in context [:request :authz-user] validity)
          (authorization-error "Invalid token" validity)))
      (catch ExceptionInfo e
        (println "error authorizing" (ex-cause e) (ex-message e))
        (authorization-error)))))

(def authz-user
  {:name ::authz-user
   :enter (authz c.users/verify-token-n-user)})

(def authz-user-gateway
  {:name ::authz-user-gateway
   :enter (authz c.users/verify-token)})

(def authz-admin
  {:name ::authz-admin
   :enter (fn [context]
            (try
              (let [token (get-in context [:request :headers "x-token"] "")]
                (cond (= token configs/admin-passphrase) context
                      :else (authorization-error)))
              (catch ExceptionInfo e
                (println "error authorizing" (ex-cause e) (ex-message e))
                (authorization-error))))})

(def json-public-interceptors [service-error-handler
                               (body-params/body-params)
                               (json-out)
                               http/html-body])

(def json-user-interceptors
  "interceptors for fully logged features, when a token for user is required,
  user is checked and email verified is mandatory"
  [service-error-handler
   (body-params/body-params)
   authz-user
   (json-out)
   http/html-body])

(def json-gateway-interceptors
  "interceptors for intermediate steps, when a token for user is required,
  user is checked but email verified is not mandatory"
  [service-error-handler
   (body-params/body-params)
   authz-user-gateway
   (json-out)
   http/html-body])

(def json-root-interceptors [service-error-handler
                             (body-params/body-params)
                             authz-admin
                             (json-out)
                             http/html-body])
