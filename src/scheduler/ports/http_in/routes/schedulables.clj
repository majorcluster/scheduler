(ns scheduler.ports.http-in.routes.schedulables
  (:require
   [pedestal-api-helper.params-helper :as ph]
   [scheduler.adapters.schedulables :as a.schedulables]
   [scheduler.configs :as configs]
   [scheduler.controllers.schedulables :as c.schedulables]
   [scheduler.logic.schedulables :as l.schedulables]
   [scheduler.ports.http-in.routes.interceptors :as i]))

(defn post
  [request]
  (let [_ (-> (get request :json-params {})
              (ph/validate-and-mop!!
               {"name"            [{:validate/type :validate/mandatory}
                                   {:validate/type :validate/min, :validate/value 3}
                                   {:validate/type :validate/max, :validate/value 100}]
                "description"     [{:validate/type :validate/max, :validate/value 255
                                    :validate/ignore-if-absent true}]
                "datetime-ranges" [{:validate/type :validate/mandatory}
                                   {:validate/type  :validate/regex-seq
                                    :validate/value configs/date-range-regex}
                                   {:validate/type  :validate/custom
                                    :validate/value (fn [value]
                                                      (try
                                                        (let [parsed (l.schedulables/parse-interval value)
                                                              _      (l.schedulables/check-for-intersections
                                                                      parsed)]
                                                          true)
                                                        (catch Exception _
                                                          false)))
                                    :validate/message "Field datetime-ranges is not valid"}]}
               ["name", "description", "datetime-ranges"])
              a.schedulables/in->internal
              c.schedulables/create)]
    {:status 204}))

(defn patch
  [request]
  (let [_ (-> (get request :json-params {})
              (ph/validate-and-mop!!
               {"id"              [{:validate/type :validate/mandatory
                                    :validate/message "Id was not provided"}
                                   {:validate/type :validate/custom
                                    :validate/value ph/is-uuid
                                    :validate/message ":id needs to match uuid v4 pattern"}]
                "name"            [{:validate/type :validate/min :validate/value 3
                                    :validate/ignore-if-absent true}
                                   {:validate/type :validate/max :validate/value 100
                                    :validate/ignore-if-absent true}]
                "description"     [{:validate/type :validate/max :validate/value 255
                                    :validate/ignore-if-absent true}]
                "datetime-ranges" [{:validate/type  :validate/regex-seq
                                    :validate/value configs/date-range-regex
                                    :validate/ignore-if-absent true}
                                   {:validate/type  :validate/custom
                                    :validate/value (fn [value]
                                                      (try
                                                        (let [parsed (l.schedulables/parse-interval value)
                                                              _      (l.schedulables/check-for-intersections
                                                                      parsed)]
                                                          true)
                                                        (catch Exception _
                                                          false)))
                                    :validate/message "Field datetime-ranges is not valid"
                                    :validate/ignore-if-absent true}]}
               ["id", "name", "description", "datetime-ranges"])
              a.schedulables/in->internal
              c.schedulables/update)]
    {:status 204}))

(defn all
  [_]
  (let [schedulables (c.schedulables/all)]
    {:status 200
     :body {:schedulables schedulables}}))

(defn by-id
  [request]
  (let [id (get-in request [:path-params :id])]
    {:status 200 :body {:schedulable
                        (c.schedulables/by-id id)}}))

(def specs
  #{["/schedulables"     :post  (conj i/json-user-interceptors `post)]
    ["/schedulables"     :patch (conj i/json-user-interceptors `patch)]
    ["/schedulables"     :get   (conj i/json-user-interceptors `all)]
    ["/schedulables/:id" :get   (conj i/json-user-interceptors `by-id)]})
