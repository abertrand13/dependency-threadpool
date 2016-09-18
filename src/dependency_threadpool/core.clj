(ns dependency-threadpool.core)

(use '(clojure pprint))

(import '(java.util.concurrent Executors))
(import '(java.util UUID))
(import '(java.util.concurrent TimeUnit))

(def debug-output-on false)
(def default-num-threads 10)
(def pool (atom (Executors/newFixedThreadPool default-num-threads)))
(def running-functions (atom #{})) ; set of running functions
(def queued-functions (atom {}))  ; map of uid's of running functions to a list of other functions that are waiting on them
(def no-queued-functions (atom (promise)))

(defn- debug [& args]
  (if debug-output-on (.write *out* (str (clojure.string/join " " args) "\n")))) 

(defn queue [task & dependency] 
  
  (let
    [uid (UUID/randomUUID)
     dep  (first dependency)
     wrapped-function (fn []
                        ; run the given task 
                        (task)
                        ; take this function out of the list of running functions
                        (swap! running-functions disj uid)

                        (locking running-functions
                          (dorun (map
                            (fn [task]
                              (.submit @pool task))
                          (get @queued-functions uid)))
                          (swap! queued-functions dissoc uid))

                        ; trigger if main thread is waiting for all functions to be in pool
                        (if (empty? @queued-functions) 
                          (do
                            (deliver @no-queued-functions true)))
                        )]  
    (swap! running-functions conj uid)
    (locking running-functions 
      (if (and dep (some #{dep} @running-functions))
        ; contains dependency (ie dependency is currently running or queued)
        (do
          (reset! no-queued-functions (promise))
          (swap! queued-functions
                 (fn [queued-functions]
                   ; associate the things (update the map of ids -> dependee functions)
                   (assoc queued-functions dep (conj (get queued-functions dep) wrapped-function))
                   )))
        ; does not contain dependency
        (do
          (.submit @pool wrapped-function)
          )))
    uid
    ))

; SETUP AND DECONSTRUCTION FUNCTIONS

(defn shutdown []
  (.shutdown @pool)
  )

(defn initialize [& numthreads]
  ; add check to make sure it doesn't already exist?
  (let [arg     (first numthreads)
        threads (if (nil? arg) default-num-threads arg)
        ]
    (reset! pool (Executors/newFixedThreadPool threads))))

; UTILITY
(defn wait-for-queueage []
  ; also need to think about a possible lock - take in a timeout, and return true/false if queue
  ; is actually empty?

  ; some weird atom/promise-y shiz.
  ; block until it's available then reset
  (if (deref @no-queued-functions) (reset! no-queued-functions (promise))))

(defn await-termination [& ms]
  (.awaitTermination @pool (if (nil? ms) 1000 (first ms)) TimeUnit/MILLISECONDS)
  )
