(ns markov.chainz-test
  (:use midje.sweet))


(def msgs ["this is a simple test message."
           "here's another test message."
           ;; "this is message which ends with hail satan!"
           ;; "hadoop is literally the worst."
           ;; "no one knows how computers work."
           ])

(future-facts "about building chains")
(future-facts "about generaring chains")
