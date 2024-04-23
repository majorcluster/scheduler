(ns scheduler.ports.http-out.core
  (:require
   [org.httpkit.client :as client]
   [org.httpkit.sni-client :as sni-client]))

(defn start
  []
  (alter-var-root #'client/*default-client* (fn [_] sni-client/default-client)))
