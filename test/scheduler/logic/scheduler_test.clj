(ns scheduler.logic.scheduler-test
  (:require
   [clojure.test :refer :all]
   [core-test :refer :all]
   [scheduler.configs :as configs]
   [scheduler.logic.schedulables :as logic.schedulables]))

;60m:8-20:mon-fri,50m:8-18:sat-2
;60m:8-20:mon-fri,50m:8-18:sat-2,60m:8-12:sun
;30m:8-20:mon-sat:12-13
;50m:1-00:all-days,not-dec24-dec31
;60m:8-20:fri
;09m:8-20:fri
;60m:8-20:mon-fri
;60m:8-20:mon-fri,
;50m:1-00:all-days
;60m:8-20:mon-fri,50m:8-18:tue-sat
;9m:8-20:fri

(deftest parse-timerange-test
  (testing "perfect day range"
    (are [s result] (= result
                       (logic.schedulables/parse-timerange s))

      "60m:8-20:mon-fri"
      {:minutes 60
       :hours {:start 8
               :end 20
               :pause-start nil
               :pause-end nil}
       :days-range {:days '(1 2 3 4 5)
                    :only-weeks nil}
       :exclusions '()}

      "30m:8-20:mon-sat:12-13"
      {:minutes 30
       :hours {:start 8
               :end 20
               :pause-start 12
               :pause-end 13}
       :days-range {:days '(1 2 3 4 5 6)
                    :only-weeks nil}
       :exclusions '()}

      "50m:1-00:all-days"
      {:minutes 50
       :hours {:start 1
               :end 24
               :pause-start nil
               :pause-end nil}
       :days-range {:days '(1 2 3 4 5 6 7)
                    :only-weeks nil}
       :exclusions '()}

      "50m:1-00:all-days:23-24"
      {:minutes 50
       :hours {:start 1
               :end 24
               :pause-start 23
               :pause-end 24}
       :days-range {:days '(1 2 3 4 5 6 7)
                    :only-weeks nil}
       :exclusions '()}

      "50m:1-00:all-days:23-00"
      {:minutes 50
       :hours {:start 1
               :end 24
               :pause-start 23
               :pause-end 24}
       :days-range {:days '(1 2 3 4 5 6 7)
                    :only-weeks nil}
       :exclusions '()}

      "100m:1-00:all-days:23-00"
      {:minutes 100
       :hours {:start 1
               :end 24
               :pause-start 23
               :pause-end 24}
       :days-range {:days '(1 2 3 4 5 6 7)
                    :only-weeks nil}
       :exclusions '()}

      "09m:8-20:fri"
      {:minutes 9,
       :hours {:start 8, :end 20, :pause-start nil, :pause-end nil},
       :days-range {:days '(5), :only-weeks nil},
       :exclusions '()}

      "09m:08-20:fri"
      {:minutes 9,
       :hours {:start 8, :end 20, :pause-start nil, :pause-end nil},
       :days-range {:days '(5), :only-weeks nil},
       :exclusions '()}

      "50m:8-18:sat-2"
      {:minutes 50,
       :hours {:start 8, :end 18, :pause-start nil, :pause-end nil},
       :days-range {:days '(6), :only-weeks 2},
       :exclusions '()}))

  (testing "parse day range wrong formats"
    (are [s msg] (is (thrown-with-data? #"Parse Exception"
                                        {:message msg
                                         :type    :parse-exception}
                                        (logic.schedulables/parse-timerange s)))

      "0m:8-20:mon-fri"
      "0 minutes not allowed"

      "60m:8-25:mon-fri"
      "8-25 is not a valid hour range"

      "60m:29-24:mon-fri"
      "29-24 is not a valid hour range"

      "60m:3-1:mon-fri"
      "3-1 is not a valid hour range"

      "60m:03-03:mon-fri"
      "03-03 is not a valid hour range"

      "50m:1-00:all-days:25-24"
      "25-24 is not a valid hour range"

      "50m:1-00:all-days:24-24"
      "24-24 is not a valid hour range"

      "50m:1-00:all-days:24-25"
      "24-25 is not a valid hour range"

      "50m:1-00:all-days:10-9"
      "10-9 is not a valid hour range"

      "50m:8-18:sat-mon"
      "sat-mon is not a valid days range as mon-fri, fri, all-days, fri-2"))

  (testing "parse perfect exclusions"
    (are [s result] (= result
                       (logic.schedulables/parse-timerange s))

      "not-dec24-dec31"
      {:minutes nil, :hours nil, :days-range nil,
       :exclusions '({:day 24, :month 12}
                     {:day 31, :month 12})}

      "not-jan01-jan31"
      {:minutes nil, :hours nil, :days-range nil,
       :exclusions '({:day 1,  :month 1}
                     {:day 31, :month 1})}))

  (testing "parse exclusions wrong formats"
    (are [s msg] (is (thrown-with-data? #"Parse Exception"
                                        {:message msg
                                         :type    :parse-exception}
                                        (logic.schedulables/parse-timerange s)))

      "not-dec24-dec32"
      "dec32 is not a valid date"

      "not-dec24-dec20-dec32"
      "dec32 is not a valid date"

      "not-dec32"
      "dec32 is not a valid date"

      "not-feb30"
      "feb30 is not a valid date"

      "not-apr31"
      "apr31 is not a valid date")))

(deftest regex-validation-test
  (testing "valid"
    (are [datetime] (re-seq configs/date-range-regex
                            datetime)
      "60m:8-20:mon-fri,50m:8-18:sat-2"
      "60m:8-20:mon-fri,50m:8-18:sat-2,60m:8-12:sun"
      "30m:8-20:mon-sat:12-13"
      "50m:1-00:all-days,not-dec24-dec31"
      "60m:8-20:fri"
      "09m:8-20:fri"
      "60m:8-20:mon-fri"
      "60m:8-20:mon-fri,"
      "50m:1-00:all-days"
      "60m:8-20:mon-fri,50m:8-18:tue-sat"
      "9m:8-20:fri"
      "100m:8-20:mon-fri,50m:8-18:tue-sat"
      "9m:8-20:fri,anything"))
  (testing "invalid"
    (are [datetime] (not (re-seq configs/date-range-regex
                                 datetime))
      "60m:8-32:mon-fri,50m:8-18:sat-8"
      "30m:8-20:mon-bat:12-13"
      "60m:8-20:bat"
      "anything,anything")))

(deftest check-for-intersections-test
  (testing "no intersection"
    (are [ranges] (not (logic.schedulables/check-for-intersections
                        (logic.schedulables/parse-interval ranges)))

      "60m:8-20:mon-fri,50m:8-18:sat-2"
      "60m:8-20:mon-fri,50m:8-18:sat-2,60m:8-12:sun"
      "30m:8-20:mon-sat:12-13"
      "60m:8-20:mon-fri,"
      "9m:8-20:fri,anything"))
  (testing "intersection"
    (are [ranges msg] (is (thrown-with-data? #"Ranges Intersection"
                                             {:type    :ranges-intersection
                                              :message msg}
                                             (logic.schedulables/check-for-intersections
                                              (logic.schedulables/parse-interval ranges))))

      "60m:8-20:mon-fri,50m:8-18:mon-tue"
      "Time ranges intersect: {:minutes 60, :hours {:start 8, :end 20, :pause-start nil, :pause-end nil}, :days-range {:days (1 2 3 4 5), :only-weeks nil}, :exclusions ()} and {:minutes 50, :hours {:start 8, :end 18, :pause-start nil, :pause-end nil}, :days-range {:days (1 2), :only-weeks nil}, :exclusions ()}"

      "60m:8-20:mon-fri,50m:8-18:sat-2,60m:8-12:sat-2"
      "Time ranges intersect: {:minutes 50, :hours {:start 8, :end 18, :pause-start nil, :pause-end nil}, :days-range {:days (6), :only-weeks 2}, :exclusions ()} and {:minutes 60, :hours {:start 8, :end 12, :pause-start nil, :pause-end nil}, :days-range {:days (6), :only-weeks 2}, :exclusions ()}")))
