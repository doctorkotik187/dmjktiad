(ns doctorkotik.dmjktiad.services.verdict-test
  (:require [clojure.test :refer [deftest testing is]]
            [doctorkotik.dmjktiad.services.verdict :as verdict]))

(deftest verdict-special-overrides
  (testing "Hecarim main override"
    (let [stats {:verdict-gap 0.1 :control-pct 0.5 :first-drake-rate 0.3
                 :zero-drake-rate 0.1 :drake-differential 0.0
                 :wr-with-drakes 0.6 :wr-without-drakes 0.5
                 :top-champs [["Hecarim" 10]]}]
      (is (.contains (verdict/verdict stats) "Hecarim main detected"))))

  (testing "first drake rate too low"
    (let [stats {:verdict-gap 0.1 :control-pct 0.5 :first-drake-rate 0.1
                 :zero-drake-rate 0.1 :drake-differential 0.0
                 :wr-with-drakes 0.6 :wr-without-drakes 0.5
                 :top-champs [["LeeSin" 10]]}]
      (is (.contains (verdict/verdict stats) "First drake goes to: the enemy"))))

  (testing "zero drake rate too high"
    (let [stats {:verdict-gap 0.1 :control-pct 0.5 :first-drake-rate 0.3
                 :zero-drake-rate 0.45 :drake-differential 0.0
                 :wr-with-drakes 0.6 :wr-without-drakes 0.5
                 :top-champs [["LeeSin" 10]]}]
      (is (.contains (verdict/verdict stats) "zero drakes"))))

  (testing "drake differential very negative"
    (let [stats {:verdict-gap 0.1 :control-pct 0.5 :first-drake-rate 0.3
                 :zero-drake-rate 0.1 :drake-differential -1.5
                 :wr-with-drakes 0.6 :wr-without-drakes 0.5
                 :top-champs [["LeeSin" 10]]}]
      (is (.contains (verdict/verdict stats) "enemy jungler is sending their regards")))))

(deftest verdict-primary-tiers
  (testing "clueless tier"
    (let [stats {:verdict-gap 0.1 :control-pct 0.2 :first-drake-rate 0.3
                 :zero-drake-rate 0.1 :drake-differential 0.0
                 :wr-with-drakes 0.6 :wr-without-drakes 0.5
                 :top-champs [["LeeSin" 10]]}]
      (is (.contains (verdict/verdict stats) "Huh"))))

  (testing "based tier"
    (let [stats {:verdict-gap 0.1 :control-pct 0.7 :first-drake-rate 0.3
                 :zero-drake-rate 0.1 :drake-differential 0.0
                 :wr-with-drakes 0.6 :wr-without-drakes 0.5
                 :top-champs [["LeeSin" 10]]}]
      (is (.contains (verdict/verdict stats) "Respect")))))

(deftest verdict-suffix
  (testing "large verdict gap appends suffix"
    (let [stats {:verdict-gap 0.5 :control-pct 0.5 :first-drake-rate 0.3
                 :zero-drake-rate 0.1 :drake-differential 0.0
                 :wr-with-drakes 0.8 :wr-without-drakes 0.3
                 :top-champs [["LeeSin" 10]]}]
      (is (.contains (verdict/verdict stats) "WR jumps from 30% to 80%"))))

  (testing "small verdict gap appends investigate suffix"
    (let [stats {:verdict-gap 0.05 :control-pct 0.5 :first-drake-rate 0.3
                 :zero-drake-rate 0.1 :drake-differential 0.0
                 :wr-with-drakes 0.52 :wr-without-drakes 0.47
                 :top-champs [["LeeSin" 10]]}]
      (is (.contains (verdict/verdict stats) "Investigate further")))))
