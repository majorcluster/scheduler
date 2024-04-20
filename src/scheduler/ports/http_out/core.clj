(ns scheduler.ports.http-out.core
  (:require
   [org.httpkit.sni-client :as sni-client]
   [org.httpkit.client :as client]))

(defn start
  []
  (alter-var-root #'client/*default-client* (fn [_] sni-client/default-client)))
