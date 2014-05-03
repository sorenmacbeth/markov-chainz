(ns markov.chainz.slakov
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [compojure.core :refer [POST defroutes]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [markov.chainz :as chainz]
            [ring.adapter.jetty :as jetty])
  (:gen-class))

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

(defn maybe-message [text username channel-name chain-path]
  (let [updater (future
                  (when-not (or (.startsWith text "@slakov")
                                (= username "slackbot"))
                    (try
                      (chainz/write-chain
                       (update-chain @chain text)
                       chain-path)
                      (catch Exception e
                        (println (format "error updating chain with %s: %s" text e))))))]
    (if (and (= channel-name "general")
             (or
              (.startsWith text "@slakov")
              (<= (rand-int 100) 15)))
      (try
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {"text" (chainz/generate-text @chain 100)})}
        (catch Exception e
          (println (format "error generating text: %s" e))
          {:status 200}))
      {:status 200})))

(defroutes app-routes
  (POST "/slakov" [text user_name channel_name :as req]
        (if (and text user_name channel_name)
          (try
            (maybe-message text user_name channel_name (:chain req))
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

(defn -main [& [port chain]]
  (let [port (Integer. (or port 8000))
        chain (or chain "/tmp/markovchains")]
    (boot-chain chain)
    (jetty/run-jetty (app chain)
                     {:port port :join? false})))
