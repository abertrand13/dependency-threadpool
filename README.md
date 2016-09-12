# dependency-threadpool

A Clojure library designed as a thin layer over a threadpool for dependency specification


## Example

Say, for example, that we are given five functions which need to be run with the following constraints:
	1. `f1`, which has no depedencies
	2. `f2`, a relatively long-function which also has no dependencies
	3. `f3`, which depends on `f1` and thus needs to run after it has completed
	4. `f4`, which also depends on `f1`
	5. `f5`, which depends on `f2`

A normal thread pool will be insufficient here, as it will not be able to guarantee the proper order of execution.  We could do it synchronously, but of course this would be inefficient.

Enter this library, which allows this order of execution to be specified and executed as the following:

```
(def parallelize-with-dependencies
  (let [_      (pool/initialize) ; Default thread pool size of 10
        func1  (pool/queue f1)	 ; (queue) returns an id which can later be used to refer to f1 as a dependency
		func2  (pool/queue f2)
	   ]
	(pool/queue f3 func1) ; Note specification of dependency on f1 here
	(pool/queue f4 func1)
	(pool/queue f5 func2)
	(pool/wait-for-queueage) ; Following are routine shutdown steps)
	(pool/shutdown 5000)	
	(pool/await-termination))) ; Blocks until graceful shutdown occurs, or the timeout of 5 seconds has been reached

```

## Usage

### (initialize & numthreads)
Initializes a threadpool with `numthreads` number of threads if specified.  Defaults to 10.

### (queue [task & dependency])
Queues a task for execution in the thread pool.  If dependency is specified, ensures that the given task will run only after its dependency has finished executing.
Returns an id for the task which can be used as the `dependency` for a future call to `(queue)`.

### (wait-for-queuage)
Blocks until all queued functions have been submitted to the thread pool for execution.  Mostly used directly before a call to `(shutdown)`

### (shutdown)
Shuts down the threadpool.  Note that functions queued by the user can exist in an intermediary state in which they have been submitted to this interface, but not yet to the actual threadpool (ie, because they are waiting on a dependency to begin executing).  To wait for all functions to start running, and thus avoid killing any that are pending, use `(wait-for-queueage)`.

### (await-termination & ms)
A wrapper around Java's awaitTermination.  Blocks until all tasks in the threadpool have finished with a maximum waiting time of `ms` in milliseconds.

## Todo

- Cleanup
- Variable logging
