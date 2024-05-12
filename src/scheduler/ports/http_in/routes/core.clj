(ns scheduler.ports.http-in.routes.core
  (:require
   [scheduler.ports.http-in.routes.schedulables :as r.schedulables]
   [scheduler.ports.http-in.routes.users :as r.users]))

(def specs (set (concat r.users/specs
                        r.schedulables/specs)))
