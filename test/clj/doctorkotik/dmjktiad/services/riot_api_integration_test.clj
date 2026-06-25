(ns doctorkotik.dmjktiad.services.riot-api-integration-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [doctorkotik.dmjktiad.services.riot-api :as riot]))

(def ^:private api-key (System/getenv "RIOT_API_KEY"))

(defmacro when-key [ & body]
  `(when (some? api-key)
     ~@body))

(deftest get-account-test
  (when-key
    (testing "resolves a known player"
      (let [result (riot/get-account :na1 "duoking1" "2r1s7")]
        (is (contains? result :ok))
        (let [account (:ok result)]
          (is (string? (:puuid account)))
          (is (string? (:gameName account)))
          (is (string? (:tagLine account))))))))

(deftest get-match-ids-test
  (when-key
    (testing "fetches match IDs for a known player"
      (let [puuid "sLq3qQpGvIDNaZyWGK5to1oQDGXsGpFeFjlVvzshN1PA2cIGpAqeujzYUwplFahT5WK6KOtvIYuJwg"
            result (riot/get-match-ids :na1 puuid)]
        (is (contains? result :ok))
        (let [ids (:ok result)]
          (is (vector? ids))
          (is (pos? (count ids)))
          (is (every? string? ids)))))))

(deftest get-match-test
  (when-key
    (testing "fetches a single match"
      (let [match-id "NA1_5587724788"
            result (riot/get-match :na1 match-id)]
        (is (contains? result :ok))
        (let [match (:ok result)]
          (is (contains? match :metadata))
          (is (map? (:info match)))
          (is (contains? (:info match) :gameDuration))
          (is (contains? (:info match) :participants))
          (is (contains? (:info match) :teams)))))))
