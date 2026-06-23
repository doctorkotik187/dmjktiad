(ns doctorkotik.dmjktiad.services.riot-api-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [doctorkotik.dmjktiad.services.riot-api :as riot]))

(deftest region->cluster-test
  (testing "known platform codes map to correct clusters"
    (is (= "europe" (riot/region->cluster :euw1)))
    (is (= "europe" (riot/region->cluster :eun1)))
    (is (= "europe" (riot/region->cluster :tr1)))
    (is (= "americas" (riot/region->cluster :na1)))
    (is (= "americas" (riot/region->cluster :br1)))
    (is (= "asia" (riot/region->cluster :kr)))
    (is (= "asia" (riot/region->cluster :jp1)))
    (is (= "sea" (riot/region->cluster :oc1)))
    (is (= "sea" (riot/region->cluster :sg2))))

  (testing "string keys work too"
    (is (= "europe" (riot/region->cluster "euw1")))
    (is (= "americas" (riot/region->cluster "na1"))))

  (testing "unknown platform defaults to europe"
    (is (= "europe" (riot/region->cluster :unknown)))
    (is (= "europe" (riot/region->cluster "xyz1"))))

  (testing "case insensitive"
    (is (= "europe" (riot/region->cluster "EUW1")))
    (is (= "americas" (riot/region->cluster "NA1")))))
