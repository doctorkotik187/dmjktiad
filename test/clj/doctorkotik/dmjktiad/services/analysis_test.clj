(ns doctorkotik.dmjktiad.services.analysis-test
  (:require [clojure.test :refer [deftest testing is]]
            [doctorkotik.dmjktiad.services.analysis :as analysis]
            [doctorkotik.dmjktiad.services.riot-api :as riot-api]))

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

(defn make-match [participant]
  {:metadata {:matchId "NA1_12345"}
   :info {:gameDuration 1800
          :participants [participant]
          :teams [{:teamId 100
                   :objectives {:dragon {:kills 3 :first true}}}
                  {:teamId 200
                   :objectives {:dragon {:kills 1 :first false}}}]}})

(deftest extract-drake-data-test
  (testing "extracts drake data for a jungle game"
    (let [match (make-match jungle-participant)
          result (riot-api/extract-drake-data match test-puuid)]
      (is (map? result))
      (is (= true (:win result)))
      (is (= 3 (:team-drakes result)))
      (is (= 1 (:enemy-drakes result)))
      (is (= true (:first-drake result)))
      (is (= "LeeSin" (:champion result)))))

  (testing "returns nil for non-jungle game"
    (let [match (make-match non-jungle-participant)]
      (is (nil? (riot-api/extract-drake-data match test-puuid)))))

  (testing "returns nil for unknown puuid"
    (let [match (make-match jungle-participant)]
      (is (nil? (riot-api/extract-drake-data match "unknown-puuid"))))))

(deftest extract-all-drake-data-test
  (testing "filters to only jungle games"
    (let [matches [(make-match jungle-participant)
                   (make-match non-jungle-participant)
                   (make-match jungle-participant)]
          result (riot-api/extract-all-drake-data matches test-puuid)]
      (is (= 2 (count result)))
      (is (every? #(= "LeeSin" (:champion %)) result))
      (is (every? #(= 3 (:team-drakes %)) result)))))

(deftest compute-stats-empty
  (testing "empty games seq"
    (let [stats (analysis/compute-stats [] 100)]
      (is (= 0 (:jungle-games stats)))
      (is (nil? (:wr-overall stats)))
      (is (:insufficient-data? stats)))))

(deftest compute-stats-insufficient
  (testing "less than 15 jungle games"
    (let [games (repeat 10 {:win true :team-drakes 2 :enemy-drakes 1 :first-drake true :champion "LeeSin"})
          stats (analysis/compute-stats games 100)]
      (is (:insufficient-data? stats))
      (is (= 10 (:jungle-games stats))))))

(deftest compute-stats-all-zero-drakes
  (testing "all zero drakes"
    (let [games (repeat 20 {:win false :team-drakes 0 :enemy-drakes 0 :first-drake false :champion "LeeSin"})
          stats (analysis/compute-stats games 100)]
      (is (= 0.0 (:team-drake-rate stats)))
      (is (= 0.0 (:zero-drake-rate stats)))
      (is (= 0.0 (:avg-team-drakes stats)))
      (is (nil? (:control-pct stats))))))

(deftest compute-stats-control-pct-divide-by-zero
  (testing "control-pct excludes 0/0 games"
    (let [games [{:win true :team-drakes 0 :enemy-drakes 0 :first-drake false :champion "A"}
                 {:win true :team-drakes 2 :enemy-drakes 1 :first-drake true :champion "B"}]
          stats (analysis/compute-stats games 100)]
      (is (= 1.0 (:control-pct stats))))))

(deftest compute-stats-normal
  (testing "normal mixed dataset"
    (let [games [{:win true :team-drakes 3 :enemy-drakes 1 :first-drake true :champion "LeeSin"}
                 {:win true :team-drakes 2 :enemy-drakes 2 :first-drake true :champion "LeeSin"}
                 {:win false :team-drakes 1 :enemy-drakes 3 :first-drake false :champion "Kayn"}
                 {:win false :team-drakes 0 :enemy-drakes 2 :first-drake false :champion "Kayn"}
                 {:win true :team-drakes 4 :enemy-drakes 0 :first-drake true :champion "Hecarim"}]
          stats (analysis/compute-stats games 100)]
      (is (= 5 (:jungle-games stats)))
      (is (= 0.6 (:team-drake-rate stats)))
      (is (= 0.6 (:first-drake-rate stats)))
      (is (= 2.0 (:avg-team-drakes stats)))
      (is (= 1.6 (:avg-enemy-drakes stats)))
      (is (= 0.4 (:drake-differential stats)))
      (is (= 0.6 (:wr-overall stats)))
      (is (= [["LeeSin" 2] ["Kayn" 2] ["Hecarim" 1]] (:top-champs stats)))
      (is (not (:insufficient-data? stats))))))

(deftest compute-stats-wr-by-tier
  (testing "wr-by-tier groups correctly"
    (let [games [{:win true :team-drakes 0 :enemy-drakes 1 :first-drake false :champion "A"}
                 {:win false :team-drakes 0 :enemy-drakes 2 :first-drake false :champion "B"}
                 {:win true :team-drakes 1 :enemy-drakes 3 :first-drake false :champion "C"}
                 {:win true :team-drakes 2 :enemy-drakes 2 :first-drake true :champion "D"}
                 {:win false :team-drakes 3 :enemy-drakes 1 :first-drake true :champion "E"}
                 {:win true :team-drakes 4 :enemy-drakes 0 :first-drake true :champion "F"}]
          stats (analysis/compute-stats games 100)
          tiers (:wr-by-tier stats)]
      (is (= 4 (count tiers)))
      (is (= "0 drakes" (:label (first tiers))))
      (is (= 2 (:games (first tiers))))
      (is (= 0.5 (:wr (first tiers))))
      (is (= "4+ (Soul)" (:label (last tiers))))
      (is (= 1.0 (:wr (last tiers)))))))
