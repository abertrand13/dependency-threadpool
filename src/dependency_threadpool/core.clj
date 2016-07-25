(ns dependency-threadpool.core)

(import '(java.util.concurrent Executors))

(def pool (Executors/newFixedThreadPool 5))

(defn queue [task] 
  ; how to do this?
  ; 1. round robin through all queued things and keep trying to submit until you can
  ; 2. do we want to keep a queue of all the things we need to queue?
  ; 3. some chained system of futures
  ; Can we check what's in the pool?  Can we check what's been out of the pool? 
  ; Turns out we can't do either
  ; We can maintain a list of things in the pool?  And then cross them off as they're executed
  ; This would require wrapping every function that got queued in another function that executed the first function
  ; and then crossed the right function (id?) off the list (Does this fuck with mutability?)
  ; So: on queue, check if the dependency is still in the list.  If so, create a watcher?!
  ; Once that value is deleted then queue the next thing?
  ; Alternatively, some sort of trigger processing thing that each executed function can call when they finish to go through a list of queued
  ; functions that have dependencies and then execute any that were depending on the function that just finished.
  ; does that make sense?
  (.submit pool task) 
  )

(defn shutdown []
  (.shutdown pool)
  )

(defn -main []
  (println "Hello World!")
  (queue (fn []
           (println "Function executed")))
  (queue (fn []
           (println "Function 2 executed")))
  (shutdown)
  )

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
 
