(ns markov.chainz.slack
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(defn json-file? [file]
  (.endsWith (.getName file) ".json"))

(defn get-texts [dir num]
  (let [dir (io/file dir)
        files (filter json-file? (file-seq dir))
        lines (map #(json/parse-string (slurp %) true) (take num files))]
    (map :text
         (apply concat lines))))
