(ns doctorkotik.dmjktiad.services.analysis-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [doctorkotik.dmjktiad.services.analysis :as analysis]))

(def test-puuid "test-puuid-123")

(def jungle-participant
  {:puuid "test-puuid-123"
   :teamPosition "JUNGLE"
   :summoner1Id 11
   :summoner2Id 4
   :teamId 100
   :win true
   :championName "LeeSin"})

(def non-jungle-participant
  (assoc jungle-participant
         :teamPosition "TOP"
         :summoner1Id 4
         :summoner2Id 12))

(def no-smite-jungle
  (assoc jungle-participant
         :summoner1Id 4
         :summoner2Id 12))

(defn make-match [participant]
  {:metadata {:matchId "NA1_12345"}
   :info {:gameDuration 1800
          :participants [participant]
          :teams [{:teamId 100
                   :objectives {:dragon {:kills 3
                                        :first true}}}
                  {:teamId 200
                   :objectives {:dragon {:kills 1
                                        :first false}}}]}})

(deftest extract-drake-data-test
  (testing "extracts drake data for a jungle game"
    (let [match (make-match jungle-participant)
          result (analysis/extract-drake-data match test-puuid)]
      (is (map? result))
      (is (= "NA1_12345" (:match-id result)))
      (is (= true (:win result)))
      (is (= 100 (:team-id result)))
      (is (= 3 (:drake-kills result)))
      (is (= true (:drake-first result)))
      (is (= "LeeSin" (:champion result)))
      (is (= 1800 (:game-duration result)))))

  (testing "returns nil for non-jungle game"
    (let [match (make-match non-jungle-participant)]
      (is (nil? (analysis/extract-drake-data match test-puuid)))))

  (testing "returns nil for unknown puuid"
    (let [match (make-match jungle-participant)]
      (is (nil? (analysis/extract-drake-data match "unknown-puuid"))))))

(deftest extract-all-drake-data-test
  (testing "filters to only jungle games"
    (let [matches [(make-match jungle-participant)
                   (make-match non-jungle-participant)
                   (make-match jungle-participant)]
          result (analysis/extract-all-drake-data matches test-puuid)]
      (is (= 2 (count result)))
      (is (every? #(= "LeeSin" (:champion %)) result))
      (is (every? #(= 3 (:drake-kills %)) result)))))

  (testing "returns empty when no jungle games"
    (let [matches [(make-match non-jungle-participant)]
          result (analysis/extract-all-drake-data matches test-puuid)]
      (is (empty? result))))
