(ns scheduler.adapters.users
  (:require
   [clj-data-adapter.core :as data-adapter]
   [clojure.string :as cstr]
   [scheduler.adapters.commons :as a.commons])
  (:import
   (java.security MessageDigest)
   (java.time Instant ZoneOffset)))

(defn ^:private sha256 [string]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes string "UTF-8"))]
    (apply str (map (partial format "%02x") digest))))

(defn print->> [msg x]
  (println msg x)
  x)

(defn print-> [x msg]
  (println msg x)
  x)

(defn internal->sql-wire
  [wire]
  (->> (data-adapter/transform-keys #(->> %
                                          name
                                          cstr/lower-case
                                          ((fn [s] (cstr/replace s #"-" "_")))
                                          keyword)
                                    wire)
       (a.commons/update-when-not-nil->> :created_at a.commons/timestamp->sql-timestamp)))

(defn sql-wire->internal
  [wire]
  (-> (data-adapter/transform-keys #(->> %
                                         name
                                         cstr/lower-case
                                         ((fn [s] (cstr/replace s #"_" "-")))
                                         ((fn [s] (str "user/" s)))
                                         keyword)
                                   wire)
      (a.commons/update-when-not-nil :user/created-at #(cond (number? %) %
                                                             :else (-> %
                                                                       a.commons/inst->local-date-time
                                                                       (.toInstant (ZoneOffset/UTC))
                                                                       (.toEpochMilli))))))

(defn sign-up->internal
  [wire]
  (-> wire
      (assoc :password (sha256 (:password wire))
             :created-at (inst-ms (Instant/now))
             :role "user"
             :id (random-uuid))
      (#(data-adapter/transform-keys (partial data-adapter/kebab-key->namespaced-key "user") %))))

(defn login->internal
  [wire]
  (-> wire
      (assoc :password (sha256 (:password wire)))))

(defn change-password->internal
  [password]
  (sha256 password))
