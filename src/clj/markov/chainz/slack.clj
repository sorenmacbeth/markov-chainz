(ns markov.chainz.slack
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(defn json-file? [file]
  (.endsWith (.getName file) ".json"))

(defn get-texts [dir & {:keys [max-files filterp]}]
  (let [dir (io/file dir)
        files (filter json-file? (file-seq dir))
        lines (apply concat (map #(json/parse-string (slurp %) true) (if max-files
                                                                       (take num files)
                                                                       files)))]
    (map :text (if filterp
                 (filter filterp lines)
                 lines))))
