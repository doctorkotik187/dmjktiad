(ns doctorkotik.dmjktiad.services.verdict-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [doctorkotik.dmjktiad.services.verdict :as verdict]))

(deftest get-verdict-test
  (testing "drake rate 0.0 - 0.2 is clueless"
    (is (= "There is a dragon?!" (verdict/get-verdict 0.0)))
    (is (= "There is a dragon?!" (verdict/get-verdict 0.1)))
    (is (= "There is a dragon?!" (verdict/get-verdict 0.2))))

  (testing "drake rate 0.21 - 0.4 is suspicious"
    (is (= "Bold of you to assume they check the map." (verdict/get-verdict 0.25)))
    (is (= "Bold of you to assume they check the map." (verdict/get-verdict 0.4))))

  (testing "drake rate 0.41 - 0.6 is aware"
    (is (= "Aware of dragons. Motivated by them: unclear." (verdict/get-verdict 0.45)))
    (is (= "Aware of dragons. Motivated by them: unclear." (verdict/get-verdict 0.6))))

  (testing "drake rate 0.61 - 0.8 is decent"
    (is (= "Decent drake presence. Might even ping it." (verdict/get-verdict 0.65)))
    (is (= "Decent drake presence. Might even ping it." (verdict/get-verdict 0.8))))

  (testing "drake rate 0.81 - 1.0 is based"
    (is (= "This jungler eats drakes for breakfast. Respect." (verdict/get-verdict 0.85)))
    (is (= "This jungler eats drakes for breakfast. Respect." (verdict/get-verdict 1.0)))))
