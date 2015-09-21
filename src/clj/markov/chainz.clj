(ns markov.chainz
  (:require [clojure.string :as s]
            [bigml.sampling [simple :as simple]]
            [markov.chainz.slack :as slack]
            [markov.chainz.rocksdb :as rocks]
            [clojure.tools.cli :refer [parse-opts]])
  (:import [markov.chainz Utils])
  (:gen-class))

(def START-TOKEN "_*_")

(defn write-chain-db [db chain]
  (let [serialize #(Utils/serialize %)]
    (rocks/put db (serialize "slakov") (serialize chain))))

(defn read-chain-db [db]
  (let [serialize #(Utils/serialize %)
        deserialize #(Utils/deserialize %)]
    (deserialize (rocks/get db (serialize "slakov")))))

(defn write-chain [chain file]
  (spit file (pr-str chain)))

(defn read-chain [file]
  (read-string (slurp file)))

(defn space-tokenizer [s]
  (when s (s/split s #"\s+")))

(defn chain [len tokens]
  (let [tokens (concat (repeat len START-TOKEN) tokens)
        p (partition (inc len) 1 tokens)
        maps (map (fn [x]
                    {(take len x)
                     {(last x) 1}}) p)]
    (apply merge-with concat maps)))

(def merge-with+ (partial apply merge-with #(try
                                              (merge-with + %1 %2)
                                              (catch Exception _ {}))))

(defn build-chain
  ([len tokenize-fn texts]
     (let [maps (map #(chain len (tokenize-fn %)) texts)]
       {:len len :data (merge-with+ maps)}))
  ([len tokenize-fn texts prev-chain]
     (let [maps (cons (:data prev-chain) (map #(chain len (tokenize-fn %)) texts))]
       {:len len :data (merge-with+ maps)})))

(defn choose-next [chain k]
  (when-let [weights (get chain k)]
    (when (map? weights)
      (let [choices (keys weights)]
        (first (simple/sample choices :weigh weights))))))

(defn generate-text [chain max-words]
  (let [len (:len chain)
        chain (:data chain)]
    (loop [k (repeat len START-TOKEN)
           acc []
           n max-words]
      (let [w (choose-next chain k)
            nacc (concat acc [w])
            nk (concat (reverse (take (dec len) (reverse k))) [w])]
        (if-not (or (nil? w) (zero? n))
          (recur nk nacc (dec n))
          (s/join " " acc))))))

(def cli-options
  [["-i" "--input PATH" "Input Path"]
   ["-o" "--output FILE" "Output File"]
   ["-l" "--token-length N" "Token Length"
    :default 2
    :parse-fn #(Integer/parseInt %)]
   ["-f" "--filter-predicate PRED" "Filter predicate"
    :default nil
    :parse-fn #(read-string %)]])

(defn -main [& args]
  (let [{:keys [options args summary errors]} (parse-opts args cli-options)]
    (write-chain
     (build-chain (:token-length options)
                  space-tokenizer
                  (slack/get-texts (:input options) :filterp (:filter-predicate options)))
     (:output options))))
