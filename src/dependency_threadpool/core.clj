(ns dependency-threadpool.core)

(use '( clojure pprint))

(import '(java.util.concurrent Executors))
(import '(java.util UUID))
(import '(java.util.concurrent TimeUnit))

(def debug-output-on false)
(def pool (atom (Executors/newFixedThreadPool 5)))
(def running-functions (atom #{})) ; set of running functions
(def queued-functions (atom {}))  ; map of uid's of running functions to a list of other functions that are waiting on them
(def no-queued-functions (atom (promise)))

(defn debug [& args]
  (if debug-output-on (.write *out* (str (clojure.string/join " " args) "\n")))) 

(defn queue [task & dependency] 
  
  (let
    [uid (UUID/randomUUID)
     dep  (first dependency)
     wrapped-function (fn []
                        (debug "executing id: " uid)
                        ; (println "executing function")
                        ; run the given task 
                        (task)
                        ; (println "finished executing")
                        ; take this function out of the list of running functions
                        ;(debug "UID:" uid)
                        (debug "BEFORE" @running-functions)
                        (swap! running-functions disj uid)
                        (debug "AFTER" @running-functions)
                        ; trigger all dependee functions

                        ; (println "pre-dequeue")
                        (dorun (map
                          (fn [task]  (.submit @pool task))
                          (get @queued-functions uid)))
                        ; (debug "FUNC:" (first (get @queued-functions uid)))
                        ; (println "dequeueing") 
                        (swap! queued-functions dissoc uid)
                        ; trigger if waiting
                        (println "attempting trigger")
                        (if (empty? @queued-functions) 
                          (do
                            ; (println "delivering") 
                            (deliver @no-queued-functions true)))
                        )] 
    (swap! running-functions conj uid)
    (if (and dep (some #{dep} @running-functions))
      ;  contains dependency (ie dependency is currently running or queued)
      (do
        (debug "queueing id: " uid)
        (if (realized? @no-queued-functions) (reset! no-queued-functions (promise)))
        (swap! queued-functions
               (fn [queued-functions]
                ; associate the things (update the map of ids -> dependee functions) 
                 (assoc queued-functions dep (conj (get queued-functions dep) wrapped-function))
                 ))
        ; (println "function queued")
        ; (debug @queued-functions)
        
        )
      ; does not contain dependency
      (do 
        (debug "submitting id:" uid) 
        (.submit @pool wrapped-function)
        ; (println "function queued without dependency") 
        ))
    
    uid ; return the uid because, yea
    ))

; SETUP AND DECONSTRUCTION FUNCTIONS

(defn shutdown []
  (debug "THREADPOOL: shutting down")
  (.shutdown @pool)
  )

(defn initialize [& nthreads]
  ; add check to make sure it doesn't already exist?
  ; in an interesting quirk, variadic arguments actually get passed as an ArraySeq (hence first)
  (reset! pool (Executors/newFixedThreadPool (if (nil? nthreads) 5 (first nthreads))))
  )

; UTILITY

(defn wait-for-queueage []
  ; also need to think about a possible lock - set a timeout, and return true/false if queue
  ; is actually empty?

  ; some weird atom/promise-y shiz.
  ; block until it's available then reset
  (debug "waiting")
  ; (println @queued-functions)
  ; (println (empty? @queued-functions))
  (if (deref @no-queued-functions 1000 false) (reset! no-queued-functions (promise)))
  (clojure.pprint/pprint @queued-functions)
  )

(defn await-termination [& ms]
  (.awaitTermination @pool (if (nil? ms) 1000 (first ms)) TimeUnit/MILLISECONDS)
  )
