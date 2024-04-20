(ns scheduler.ports.http-out.email
  (:require
   [clojure.data.json :as cjson]
   [org.httpkit.client :as http]
   [outpace.config :refer [defconfig]]))

(defconfig url "my-mail-provider.com/api")
(defconfig from "test@test.com")
(defconfig api-key "my-key")
(defconfig company "my-company")

(defn send-email
  [to subject body data]
  (http/request {:url url
                 :method :post
                 :headers {"Content-Type" "application/json"
                           "X-Requested-With" "XMLHttpRequest"
                           "Authorization" (str "Bearer " api-key)}
                 :body (cjson/write-str {:from {:email from
                                                :name company}
                                         :to   [{:email to}]
                                         :subject subject
                                         :html body
                                         :personalization [{:email to
                                                            :data data}]})
                 :timeout 3000}
                (fn async-callback [{:keys [status error] :as response}]
                  (if error
                    (println "Failed, exception for " response)
                    (println "Async HTTP GET: " status)))))
