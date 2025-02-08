(ns scheduler.configs
  (:require
   [outpace.config :refer [defconfig]]))

(defconfig env)
(defconfig user-passphrase)
(defconfig admin-passphrase)
(defconfig token-secret)
(defconfig public-url "http://localhost:8080")

(def default-lang :pt-BR)

(def date-range-regex
  #"((\d{1,3})m:([0-2]?[0-9]-[0-2]?[0-9]{1}){1}:((?:(?:mon|tue|wed|thu|fri|sat|sun)-(?:mon|tue|wed|thu|fri|sat|sun)){1}|(?:(?:mon|tue|wed|thu|fri|sat|sun)-[1-4]{1}){1}|(?:mon|tue|wed|thu|fri|sat|sun|all-days){1}){1}(?::([0-2]?[0-9]-[0-2]?[0-9]{1}))?|not-((?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[0-3][0-9])(?:-((?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[0-3][0-9])){0,29})(?:,|$)")

(defn env-test?
  []
  (= "test" env))
