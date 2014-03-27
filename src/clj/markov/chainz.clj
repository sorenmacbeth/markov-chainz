(ns markov.chainz
  (:require [clojure.string :as s]
            [bigml.sampling [simple :as simple]]))

(def START-TOKEN "_*_")

(defrecord Chain [len data])

(defn tokenize [s]
  (s/split s #"\s+"))

(defn chain [len tokens]
  (let [tokens (concat (repeat len START-TOKEN) tokens)
        p (partition (+ len 1) 1 tokens)
        maps (map (fn [x]
                    {(take len x)
                     {(last x) 1}}) p)]
    (apply merge-with concat maps)))

(def merge-with+ (partial merge-with #(merge-with + %1 %2)))

(defn build-chain
  ([len tokenize-fn texts]
     (let [maps (map #(chain len (tokenize-fn %)) texts)]
       (->Chain len (apply merge-with+ maps))))
  ([len tokenize-fn texts chains]
     (let [maps (cons chains (map #(chain len (tokenize %)) texts))]
       (->Chain len (apply merge-with+ maps)))))

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
