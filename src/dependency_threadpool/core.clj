(ns dependency-threadpool.core)

(import '(java.util.concurrent Executors))
(import '(java.util UUID))
(import '(java.util.concurrent TimeUnit))

(def pool (atom (Executors/newFixedThreadPool 5)))
(def running-functions (atom #{})) ; set of running functions
(def queued-functions (atom {}))  ; map of uid's of running functions to a list of other functions that are waiting on them

(defn debug [& args]
  (.write *out* (str (clojure.string/join " " args) "\n"))) 

(defn queue [task & dependency] 
  
  (let
    [uid (UUID/randomUUID)
     dep  (first dependency)
     wrapped-function (fn []
                        (debug "executing id: " uid)
                        ; run the given task 
                        (task)
                        ; take this function out of the list of running functions
                        ;(debug "UID:" uid)
                        (debug "BEFORE" @running-functions)
                        (swap! running-functions disj uid)
                        (debug "AFTER" @running-functions)
                        ; trigger all dependee functions

                        (apply
                          (fn [task] (debug "task:" task) (task) (.submit @pool task))
                          (get @queued-functions uid))
                        ; (debug "FUNC:" (first (get @queued-functions uid)))
                        (swap! queued-functions dissoc uid)
                        )] 
    (swap! running-functions conj uid)
    (if (and dep (some #{dep} @running-functions))
      ; contains dependency (ie dependency is currently running or queued)
      (do
        (debug "queueing id: " uid) 
        (swap! queued-functions
               (fn [queued-functions]
                ; associate the things (update the map of ids -> dependee functions) 
                 (assoc queued-functions dep (conj (get queued-functions dep) wrapped-function))
                 ))
        ; (debug @queued-functions)
        
        )
      ; does not contain dependency
      (do
        (debug "submitting id:" uid) 
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

; UTILITY
(defn wait-for-queueage []
  ; there has to be a better way to do this
  ; also need to think about a possible lock - set a timeout, and return true/false if queue
  ; is actually empty?
  (while (not (empty? @queued-functions))
    (do
     (debug "queue:" @queued-functions)
     (Thread/sleep 100)))
  )

(defn await-termination [& ms]
  (.awaitTermination @pool (if (nil? ms) 1000 (first ms)) TimeUnit/MILLISECONDS)
  )

(defn -main []
  (debug "Hello World!")
  (let [
        uid1  (queue (fn []
                       (debug "Function executed")))])
  (queue (fn []
           (debug "Function 2 executed")))
  (shutdown)
  )


