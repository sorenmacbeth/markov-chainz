(defproject markov-chainz "0.1.0-SNAPSHOT"
  :description "simple library for building text-based markov chains"
  :url "https://github.com/sorenmacbeth/markov-chainz"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [bigml/sampling "2.1.0"]
                 [cheshire "5.3.1"]
                 [compojure "1.1.6"]
                 [ring/ring "1.2.2"]
                 ]
  :source-paths ["src/clj"]
  :jvm-opts ["-server" "-Xms1g" "-Xmx1g"]
  :main markov.chainz.web
  :ring {:handler markov.chainz.web/app}
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [ring-mock "0.1.5"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-ring "0.8.10"]]}
             :uberjar {:aot :all}})
