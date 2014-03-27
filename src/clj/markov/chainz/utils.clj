(ns markov.chainz.utils)

(defn write-chain [chain file]
  (spit file (pr-str chain)))

(defn read-chain [file]
  (read-string (slurp file)))
