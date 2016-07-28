(ns dependency-threadpool.core)

(import '(java.util.concurrent Executors))
(import '(java.util UUID))
(import '(java.util.concurrent TimeUnit))

(def pool (atom (Executors/newFixedThreadPool 5)))
(def running-functions (atom #{})) ; set of running functions
(def queued-functions (atom {}))  ; map of uid's of running functions to a list of other functions that are waiting on them

(defn queue [task & dependency] 
  
  (let
    [uid (UUID/randomUUID)
     dep  (first dependency)
     wrapped-function (fn []
                        (println "executing id: " uid)
                        ; run the given task 
                        (task)
                        ; take this function out of the list of running functions
                        ;(println "UID:" uid)
                        ;(println "BEFORE" @running-functions)
                        (swap! running-functions disj uid)
                        ;(println "AFTER" @running-functions)
                        ; trigger all dependee functions

                        (apply
                          (fn [task] (.submit @pool task))
                          (get @queued-functions uid))
                        ; (println "FUNC:" (first (get @queued-functions uid)))
                        (swap! queued-functions dissoc uid)
                        )] 
    (swap! running-functions conj uid)
    (if (and dep (some #{dep} @running-functions))
      ; contains dependency (ie dependency is currently running or queued)
      (do
        (swap! queued-functions
               (fn [queued-functions]
                ; associate the things (update the map of ids -> dependee functions) 
                 (assoc queued-functions dep (conj (get queued-functions dep) wrapped-function))
                 ))
        ; (println @queued-functions)
        
        )
      ; does not contain dependency
      (do
        (println "submitting") 
        (.submit @pool wrapped-function)))
    
    uid ; return the uid because, yea
    ))

; SETUP AND DECONSTRUCTION FUNCTIONS

(defn shutdown []
  (.shutdown @pool)
  )

(defn initialize [& nthreads]
  ; add check to make sure it doesn't already exist?
  ; in an interesting quirk, variadic arguments actually get passed as an ArraySeq (hence first)
  (reset! pool (Executors/newFixedThreadPool (if (nil? nthreads) 5 (first nthreads))))
  )

(defn wait [& ms]
  (.awaitTermination @pool (if (nil? ms) 1000 (first ms)) TimeUnit/MILLISECONDS)
  )

(defn -main []
  (println "Hello World!")
  (let [
        uid1  (queue (fn []
                       (println "Function executed")))])
  (queue (fn []
           (println "Function 2 executed")))
  (shutdown)
  )
