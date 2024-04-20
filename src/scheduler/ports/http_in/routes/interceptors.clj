(ns scheduler.ports.http-in.routes.interceptors
  (:require
   [clj-data-adapter.core :as data-adapters]
   [clojure.data.json :as cjson]
   [scheduler.configs :as configs]
   [scheduler.controllers.users :as c.users])
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
