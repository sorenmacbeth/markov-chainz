(ns markov.chainz.web
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
  (reset! chain (chainz/read-chain path)))

(defn update-chain [chain text]
  (let [new-chain (chainz/build-chain
                   (:len chain)
                   chainz/space-tokenizer
                   [text]
                   chain)]
    (reset! chain new-chain)
    new-chain))

(defn handle-message [req]
  (let [form-params (:form-params req)
        updater (future
                  (chainz/write-chain (update-chain @chain (get form-params "text")) (:chains req)))]
    (if (<= (rand-int 100) 15)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {"text" (chainz/generate-text @chain 100)})
       }
      {:status 200})))

(defroutes app-routes
  (POST "/slakov" [] handle-message)
  (route/resources "/")
  (route/not-found "Not Found"))

(defn wrap-chain [app chains]
  (fn [req]
    (app (assoc req :chains chains))))

(defn app [chains]
  (-> app-routes
      (wrap-chain chains)
      (handler/api)))

(defn -main [& [port chains]]
  (let [port (Integer. (or port 8000))
        chains (or chains "/tmp/markovchains")]
    ;; (alter-var-root (var chain) #(reset! % (chainz/read-chain chains)))
    (boot-chain chains)
    (jetty/run-jetty (app chains)
                     {:port port :join? false})))
