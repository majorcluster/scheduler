(ns scheduler.configs
  (:require
   [outpace.config :refer [defconfig]]))

(defconfig env)
(defconfig user-passphrase)
(defconfig admin-passphrase)
(defconfig token-secret)
(defconfig public-url "http://localhost:8080")
(def default-lang :pt-BR)

(defn env-test?
      []
      (= "test" env))
