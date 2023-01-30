(ns async.core)

(def lock [])

(def wait-for-function-helper
  (memoize (fn [f args]
             (let [answer (promise)]
               (println "waiting for function " f " with args" args)
               (future (deliver answer (apply f args)))
               answer))))

(defn wait-for-function [& args]
  (locking lock
    (apply wait-for-function-helper args)))

(defn dependent-func [f g & args]
  @(wait-for-function f args)
  (apply g args))

(defn slow-f-1 [x]
  (println "starting slow-f-1")
  (Thread/sleep 10000)
  (println "finishing slow-f-1")
  (dec x))

(do (future
      (println "first" (dependent-func slow-f-1 inc 4)))
    (future
      (println "second" (dependent-func slow-f-1 inc 4))))
