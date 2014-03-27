(defproject markov-chainz "0.1.0-SNAPSHOT"
  :description "simple library for building text-based markov chains"
  :url "https://github.com/sorenmacbeth/markov-chainz"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [bigml/sampling "2.1.0"]]
  :source-paths ["src/clj"]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]]}})
