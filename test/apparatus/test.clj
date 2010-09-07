(ns apparatus.test
  (:use [clojure.test])
  (:require [apparatus.config :as config]
            [apparatus.cluster :as cluster])
  (:import [java.util UUID]))

(defn many [f] (take 4 (repeatedly f)))

(deftest apparatus
  (testing "apparatus"
    (let [instances (doall (many #(cluster/instance (config/default))))]
      (testing "should be able to eval an sexp"
        (testing "on randomly selected member"
          (doall
           (many
            #(let [sexp `(* ~(rand) ~(rand-int 100))]
               (is (= (eval sexp) (-> (cluster/eval sexp) (.get))))))))
        (testing "on a member that owns a key"
          (doall
           (many
            #(let [uuid (str (UUID/randomUUID))
                   sexp `(* ~(rand) ~(rand-int 100))]
               (-> (cluster/set "test") (.add uuid))
               (is (= (eval sexp)
                      (-> (cluster/eval sexp uuid) (.get))))))))
        (testing "on a member by reference"
          (doall
           (many
            #(let [sexp `(* ~(rand) ~(rand-int 100))]
               (is (= (eval sexp)
                      (-> (cluster/eval sexp (first (cluster/members)))
                          (.get))))))))
        (testing "on some of the members"
          (doall
           (many
            #(let [sexp `(* ~(rand) ~(rand-int 100))]
               (is (= (eval sexp)
                      (-> (cluster/eval sexp (drop 2 (cluster/members)))
                          (.get))))))))
        (testing "on all members"
          (doall
           (many
            #(let [sexp `(* ~(rand) ~(rand-int 100))]
               (is (= (eval sexp)
                      (-> (cluster/eval sexp (cluster/members))
                          (.get)))))))))
      (testing "with a distributed map")
      (testing "with a distributed mmap")
      (testing "with a distributed set"
        (let [colors ["red" "green" "blue"]
              set (cluster/set "colors")]
          (doseq [color colors] (-> set (.add color)))
          (testing "should only ever contain one of each entry"
            (doseq [color colors] (-> set (.add color)))
            (is (= (count colors) (count (cluster/set "colors")))))
          (testing "should be uniform across all members"
            (is (= (count colors)
                   (-> (cluster/eval
                        `(do (require '[apparatus.cluster :as cluster])
                             (count (cluster/set "colors")))
                        (cluster/members))
                       (.get)))))))
      (testing "with a distributed list")
      (testing "with a distributed queue")
      (testing "with a distributed topic")
      (testing "with a distributed lock")
      (testing "with a distributed transaction")
      (cluster/shutdown))))