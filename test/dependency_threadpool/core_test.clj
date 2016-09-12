(ns dependency-threadpool.core-test
  (:require [clojure.test :refer :all]
            [dependency-threadpool.core :refer :all :as pool]))

(deftest ^:simple simple-test
  (let [_     (pool/initialize)
        string (atom "") 
        uid1  (pool/queue (fn []
                            (swap! string str "ab")
                            ; (println @string)

                            ))
        uid2  (pool/queue (fn []
                            (swap! string str "cd")
                            ;(println @string)

                            ) uid1)
        uid3  (pool/queue (fn []
                            (swap! string str "ef")
                            ; (println @string)
                            
                            ) uid2)
        uid4  (pool/queue (fn []
                            (swap! string str "gh")
                            ; (println @string)
                            
                            ) uid3)
        _     (pool/wait-for-queueage)
        _     (pool/shutdown)
        _     (pool/await-termination 1000)
        ] 
    (testing "Simple Test 1 :: One dep chain, 4 functions"
      (is (= @string "abcdefgh")))))


(deftest medium-test
  (testing "Two dep chains, 3 functions each"
    (let [_       (pool/initialize)
          string1 (atom "")
          string2 (atom "")
          uid1a   (pool/queue (fn [] (swap! string1 str "ab")))
          uid2a   (pool/queue (fn [] (swap! string2 str "gh")))
          uid1b   (pool/queue (fn [] (swap! string1 str "cd")) uid1a)
          uid2b   (pool/queue (fn [] (swap! string2 str "ij")) uid2a)
          uid1c   (pool/queue (fn [] (swap! string1 str "ef")) uid1b)
          uid2c   (pool/queue (fn [] (swap! string2 str "kl")) uid2b)
          _       (pool/wait-for-queueage) 
          _       (pool/shutdown)
          _       (pool/await-termination 1000)
          ]
      (is (= @string1 "abcdef"))
      (is (= @string2 "ghijkl"))))
  )

(deftest sleep-test
  (testing "One dependency chain with sleeps in the middle"
    (let [_       (pool/initialize)
          string  (atom "")
          uid1    (pool/queue (fn [] (swap! string str "ab") (Thread/sleep 1000)))
          uid2    (pool/queue (fn [] (swap! string str "cd") (Thread/sleep 1000)) uid1)
          uid3    (pool/queue (fn [] (swap! string str "ef") (Thread/sleep 1000)) uid2)
          _       (pool/wait-for-queueage)
          _       (pool/shutdown)
          _       (pool/await-termination)
          ]
      (is (= @string "abcdef"))))
  )
