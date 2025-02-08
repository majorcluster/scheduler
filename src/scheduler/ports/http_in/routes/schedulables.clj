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
                                                          false)))}]}
               ["name", "description", "datetime-ranges"])
              a.schedulables/in->internal
              c.schedulables/create)]
    {:status 204}))

(def specs
  #{["/schedulables" :post (conj i/json-user-interceptors `post)]})
