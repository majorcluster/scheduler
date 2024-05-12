(ns scheduler.ports.http-in.routes.schedulables
  (:require
   [pedestal-api-helper.params-helper :as ph]
   [scheduler.adapters.schedulables :as a.schedulables]
   [scheduler.controllers.schedulables :as c.schedulables]
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
                                   {:validate/type :validate/regex
                                    :validate/value #"(@(annually|yearly|monthly|weekly|daily|hourly|reboot))|(@every (\d+(ns|us|µs|ms|s|m|h))+)|((((\d+,)+\d+|(\d+(\/|-)\d+)|\d+|\*) ?){5,7})"}]}
               ["name", "description", "datetime-ranges"])
              a.schedulables/in->internal
              c.schedulables/create)]
    {:status 204}))

(def specs
  #{["/schedulables" :post (conj i/json-user-interceptors `post)]})
