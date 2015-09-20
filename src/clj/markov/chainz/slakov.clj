(ns markov.chainz.slakov
  (:require [markov.chainz :as chainz]
            [markov.chainz.rtm :as rtm]
            [markov.chainz.rtm.team :as team]
            [markov.chainz.rtm.receive :as rx]
            [markov.chainz.rtm.api :as slack]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [manifold.stream :as s]
            [bigml.sampling.simple :as simple]
            [clojure.tools.cli :refer [parse-opts]]
            [environ.core :refer [env]])
  (:gen-class))

(def SLACK-TOKEN (env :slack-token))
(def UPDATE-CHAIN (Boolean/valueOf (env :update-chain "true")))
(def SPEAK-PROBABILITY (Integer/parseInt (env :speak-probability "15")))
(def MAX-WORDS (Integer/parseInt (env :max-words "25")))

(def chain (atom {}))

(defn boot-chain [path]
  (println (format "booting chain from %s" path))
  (reset! chain (chainz/read-chain path)))

(defn update-chain [old-chain text]
  (when-let [new-chain (chainz/build-chain
                        (:len old-chain)
                        chainz/space-tokenizer
                        [text]
                        old-chain)]
    (println (format "updating chain with: %s" text))
    (reset! chain new-chain)
    new-chain))

(defn maybe-speak [chain-path update? {:keys [type text channel user ts] :as event}]
  (let [self-id (team/self-id)
        me? (= user self-id)]
    (when (and
           (= type "message")
           (not me?))
      (let [mention? (.startsWith text (str "<@" self-id ">"))
            updater (when update?
                      (future
                        (when-not (or mention?
                                      (team/bot? user))
                          (try
                            (chainz/write-chain
                             (update-chain @chain text)
                             chain-path)
                            (catch Exception e
                              (println (format "error updating chain with %s: %s" text e)))))))]
        (when (or mention?
                  (<= (rand-int 100) SPEAK-PROBABILITY))
          (let [balderdash (chainz/generate-text @chain MAX-WORDS)]
            (if (< (mod (rand-int 100) 2) 1)
              (do
               (println "saying:" balderdash)
               (slack/chat-post-message SLACK-TOKEN channel balderdash :as_user true))
              (let [all-emoji (concat slack/emoji (->> (slack/emoji-list SLACK-TOKEN) keys (map name)))
                    emoji (first (simple/sample all-emoji))]
                (println "adding reaction:" emoji)
                (slack/reactions-add SLACK-TOKEN emoji channel ts)))))))))

(def disconnect rtm/stop-real-time!)

(defn connect
  ([api-token handle-event-fn]
   (connect api-token handle-event-fn {:log true}))
  ([api-token handle-event-fn options]
   (let [rx-event-stream (s/stream 16)
         slakov-event-stream (s/stream 16)
         reconnect #(do
                      (when (:log options)
                        (println "reconnecting..."))
                      (disconnect)
                      (connect api-token handle-event-fn options))]
     (s/consume #(rx/handle-event % (fn [d] (s/put! slakov-event-stream d))) rx-event-stream)
     (s/consume handle-event-fn slakov-event-stream)
     (rtm/start-real-time! api-token team/team-state! #(s/put! rx-event-stream %) reconnect options))))

(def cli-options
  [["-c" "--chain PATH" "Chain Database Path"
    :default "/tmp/markovchains"]])

(defn -main [& args]
  (let [{:keys [options arguments summary errors]} (parse-opts args cli-options)]
    (boot-chain (:chain options))
    (connect SLACK-TOKEN #(maybe-speak (:chain options) UPDATE-CHAIN %))))
