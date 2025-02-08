(ns scheduler.logic.schedulables
  (:require
   [clojure.string :as cstr]
   [scheduler.configs :as configs]))

(defn- is-absent-and-ignored
  [body field-ks igore-if-absent?]
  (and igore-if-absent? (not (contains? body field-ks))))

(defn validate-regex
  ([body field-name validation-value raw-message igore-if-absent?]
   (let [field-ks (keyword field-name)
         field-value (field-ks body)
         valid? (cond (is-absent-and-ignored body field-ks igore-if-absent?) true
                      (string? field-value) (re-matches validation-value field-value)
                      :else false)]
     (cond valid? {:validate/field field-name
                   :validate/valid true}
           :else {:validate/field field-name
                  :validate/result-message (format raw-message field-name),
                  :validate/valid false})))
  ([body field-name validation-value igore-if-absent?]
   (validate-regex body field-name validation-value "Field %s is not valid" igore-if-absent?)))

(def ^:private weekdays
  {"mon" 1
   "tue" 2
   "wed" 3
   "thu" 4
   "fri" 5
   "sat" 6
   "sun" 7})

(def ^:private months
  {"jan" 1 "feb" 2 "mar" 3 "apr" 4 "may" 5 "jun" 6
   "jul" 7 "aug" 8 "sep" 9 "oct" 10 "nov" 11 "dec" 12})

(def ^:private months-max-cap
  {"jan" 31 "feb" 29 "mar" 31 "apr" 30 "may" 31 "jun" 30
   "jul" 31 "aug" 31 "sep" 30 "oct" 31 "nov" 30 "dec" 31})

(defn ^:private transform-date
  [date-str]
  (let [[_ month day] (re-matches #"([a-z]{3})(\d{1,2})" date-str)
        parsed-month (get months month)
        parsed-day   (Integer/parseInt day)
        valid-date?  (<= parsed-day (get months-max-cap month))]
    (if valid-date?
      {:day parsed-day
       :month parsed-month}
      (throw (ex-info "Parse Exception" {:type :parse-exception
                                         :message (str date-str " is not a valid date")})))))

(defn ^:private parse-hour-long
  [s hours]
  (let [hour (parse-long s)]
    (cond
      (= 0 hour)
      24

      (> hour 24)
      (throw (ex-info "Parse Exception" {:type :parse-exception
                                         :message (str hours " is not a valid hour range")}))

      :else
      hour)))

(defn ^:private parse-minutes
  [minutes]
  (let [minutes (parse-long minutes)]
    (if (and (> minutes 0)
             (< minutes 999))
      minutes
      (throw (ex-info "Parse Exception" {:type :parse-exception
                                         :message (str minutes " minutes not allowed")})))))

(defn ^:private parse-hours
  [hours pause-interval]
  (let [[start end] (cstr/split hours #"-")
        pause-interval-parts (cstr/split (or pause-interval "")
                                         #"-")
        parsed-start (parse-hour-long start hours)
        parsed-end   (parse-hour-long end hours)
        parsed-pause-start (if pause-interval
                             (-> pause-interval-parts
                                 first
                                 (parse-hour-long pause-interval))
                             nil)
        parsed-pause-end   (if pause-interval
                             (-> pause-interval-parts
                                 last
                                 (parse-hour-long pause-interval))
                             nil)]
    (cond
      (>= parsed-start parsed-end)
      (throw (ex-info "Parse Exception" {:type :parse-exception
                                         :message (str hours " is not a valid hour range")}))

      (and parsed-pause-start
           parsed-pause-end
           (>= parsed-pause-start
               parsed-pause-end))
      (throw (ex-info "Parse Exception" {:type :parse-exception
                                         :message (str pause-interval " is not a valid hour range")}))

      :else
      {:start parsed-start
       :end   parsed-end
       :pause-start parsed-pause-start
       :pause-end   parsed-pause-end})))

(defn ^:private parse-days-range
  [days-range]
  (cond
    (cstr/starts-with? days-range "all-days")
    {:days (range 1 8)
     :only-weeks nil}

    (re-find #"(mon|tue|wed|thu|fri|sat|sun)-[1-4]{1}" days-range)
    (let [parts (cstr/split days-range #"-")
          only-weeks (parse-long (last parts))]
      {:days (list (get weekdays (first parts)))
       :only-weeks only-weeks})

    :else
    {:days
     (let [[start end] (cstr/split days-range #"-")]
       (if end
         (let [parsed-start (get weekdays start)
               parsed-end   (inc (get weekdays end))]
           (if (<= parsed-end parsed-start)
             (throw (ex-info "Parse Exception" {:type :parse-exception
                                                :message (str days-range " is not a valid days range as mon-fri, fri, all-days, fri-2")}))

             (range parsed-start parsed-end)))
         (list (get weekdays start))))
     :only-weeks nil}))

(defn parse-timerange
  [s]
  (let [is-exclusion? (cstr/starts-with? s "not-")
        matches       (re-seq configs/date-range-regex s)]
    (cond (and matches
               is-exclusion?)
          {:minutes nil
           :hours nil
           :days-range nil
           :exclusions (mapv
                        transform-date
                        (rest (cstr/split s #"-")))}

          :else
          (->> matches
               (map (fn [[_ _ minutes hours days-range pause-interval]]
                      {:minutes
                       (parse-minutes minutes)

                       :hours
                       (parse-hours hours pause-interval)

                       :days-range
                       (parse-days-range days-range)

                       :exclusions '()}))
               first))))

(defn parse-interval
  [input]
  (mapv parse-timerange (cstr/split input #",")))

(defn ranges-overlap?
  [range1 range2]
  (let [start1 (get-in range1 [:hours :start])
        end1   (get-in range1 [:hours :end])
        start2 (get-in range2 [:hours :start])
        end2   (get-in range2 [:hours :end])]
    (and (some? start1) (some? start2)
         (or (and (<= start1 start2) (>= (dec end1) start2))
             (and (<= start2 start1) (>= (dec end2) start1))))))

(defn days-overlap? [days1 days2 only-weeks1 only-weeks2]
  (and (some? days1) (some? days2)
       (= only-weeks1 only-weeks2)
       (not (empty? (clojure.set/intersection (set days1) (set days2))))))

(defn exclusions-apply?
  [exclusions day-to-compare]
  ;; If exclusions are empty or nil, treat as no exclusion conflict
  (or (nil? exclusions)
      (empty? exclusions)
      (not-any? (fn [{:keys [day]}] (= day day-to-compare)) exclusions)))

(defn time-ranges-overlap?
  [range1 range2]
  (let [days1 (get-in range1 [:days-range :days])
        only-weeks1 (get-in range1 [:days-range :only-weeks])
        days2 (get-in range2 [:days-range :days])
        only-weeks2 (get-in range2 [:days-range :only-weeks])
        exclusions1 (:exclusions range1)
        exclusions2 (:exclusions range2)
        ranges-overlap (ranges-overlap? range1 range2)
        days-overlap  (days-overlap? days1 days2 only-weeks1 only-weeks2)
        exclusions-apply-1 (exclusions-apply? exclusions1 days1)
        exclusions-apply-2 (exclusions-apply? exclusions2 days2)]
    {:result
     (and ranges-overlap
          days-overlap
          exclusions-apply-1
          exclusions-apply-2)
     :explain {:ranges-overlap ranges-overlap
               :days-overlap days-overlap
               :exclusions-apply-1 exclusions-apply-1
               :exclusions-apply-2 exclusions-apply-2}}))

(defn check-for-intersections
  [ranges]
  (loop [remaining ranges]
    (when (seq remaining)
      (let [current (first remaining)
            rest-ranges (rest remaining)]
        (doseq [other rest-ranges]
          (let [overlap-result (time-ranges-overlap? current other)]
            (when (:result overlap-result)
              (throw (ex-info "Ranges Intersection"
                              {:type :ranges-intersection
                               :message (str "Time ranges intersect: " current " and " other)})))))
        (recur rest-ranges)))))
