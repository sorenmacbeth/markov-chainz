(ns markov.chainz.slakov
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [compojure.core :refer [POST defroutes]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [markov.chainz :as chainz]
            [ring.adapter.jetty :as jetty]
            [clojure.tools.cli :refer [parse-opts]]
            [environ.core :refer [env]])
  (:gen-class))

(def chain (atom {}))

(def BOT-NAME (env :bot-name))
(def LISTEN-CHANNEL (env :listen-channel))
(def UPDATE-CHAIN (Boolean/valueOf (env :update-chain)))
(def SPEAK-PROBABILITY (Integer/parseInt (env :speak-probability)))
(def MAX-WORDS (Integer/parseInt (env :max-words)))

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

(defn maybe-message [text username channel-name chain-path update?]
  (let [updater (when update?
                  (future
                    (when-not (or (.startsWith text (str "@" BOT-NAME))
                                  (= username "slackbot"))
                      (try
                        (chainz/write-chain
                         (update-chain @chain text)
                         chain-path)
                        (catch Exception e
                          (println (format "error updating chain with %s: %s" text e)))))))]
    (if (and (= channel-name LISTEN-CHANNEL)
             (or
              (.startsWith text (str "@" BOT-NAME))
              (<= (rand-int 100) SPEAK-PROBABILITY)))
      ;; TODO: load a map of keyword/response out of resources/
      (cond
       (.contains (.toLowerCase text) "campari")
       (try
         {:status 200
          :headers {"Content-Type" "application/json"}
          :body (json/generate-string {"text" "http://media.giphy.com/media/X91oeDLYRhHEs/giphy.gif"})}
         (catch Exception e
           (println (format "error getting campari: %s" e))
           {:status 200}))
       (.contains (.toLowerCase text) "sandwich")
       (try
         {:status 200
          :headers {"Content-Type" "application/json"}
          :body (json/generate-string {"text" "http://giphy.com/gifs/JtIMv7cMRUFX2"})}
         (catch Exception e
           (println (format "error getting sandwich: %s" e))
           {:status 200}))
       :else
       (try
         {:status 200
          :headers {"Content-Type" "application/json"}
          :body (json/generate-string {"text" (chainz/generate-text @chain MAX-WORDS)})}
         (catch Exception e
           (println (format "error generating text: %s" e))
           {:status 200})))
      {:status 200})))

(defroutes app-routes
  (POST "/slakov" [text user_name channel_name :as req]
        (if (and text user_name channel_name)
          (try
            (maybe-message text user_name channel_name (:chain req) UPDATE-CHAIN)
            (catch Exception e
              (println (format "error processing message %s: %s" req e))))
          (println (format "missing required field in message %s" req))))
  (route/resources "/")
  (route/not-found "Not Found"))

(defn wrap-chain [app chain]
  (fn [req]
    (app (assoc req :chain chain))))

(defn app [chain]
  (-> app-routes
      (wrap-chain chain)
      (handler/api)))

(def cli-options
  [["-p" "--port PORT" "Port Number"
    :default 8000
    :parse-fn #(Integer/parseInt %)]
   ["-c" "--chain PATH" "Chain Database Path"
    :default "/tmp/markovchains"]])

(defn -main [& args]
  (let [{:keys [options arguments summary errors]} (parse-opts args cli-options)]
    (boot-chain (:chain options))
    (jetty/run-jetty (app (:chain options))
                     {:port (:port options) :join? false})))
