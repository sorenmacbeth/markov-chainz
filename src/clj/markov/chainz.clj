(ns markov.chainz
  (:require [clojure.string :as s]
            [bigml.sampling [simple :as simple]]))

(def START-TOKEN "_*_")

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

(defn choose-next [chains k]
  (when-let [weights (get chains k)]
    (let [choices (keys weights)]
      (first (simple/sample choices :weigh weights)))))

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
