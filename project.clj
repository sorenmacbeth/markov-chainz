(defproject markov-chainz "0.2.0-SNAPSHOT"
  :description "simple library for building text-based markov chains"
  :url "https://github.com/sorenmacbeth/markov-chainz"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [bigml/sampling "2.1.0"]
                 [cheshire "5.5.0"]
                 [aleph "0.4.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [clj-time "0.11.0"]
                 [environ "1.0.0"]]
  :source-paths ["src/clj"]
  :jvm-opts ["-Xmx4g"]
  :main markov.chainz.slakov
  :profiles {:dev {:dependencies [[midje "1.7.0"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-environ "1.0.0"]]
                   :env {:update-chain "true"
                         :speak-probability "15"
                         :max-words "25"}}
             :uberjar {:aot :all}})
