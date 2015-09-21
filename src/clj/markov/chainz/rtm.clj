(ns markov.chainz.rtm
  (:require [cheshire.core :as json]
            [aleph.http :as http]
            [byte-streams :as bs]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [clojure.core.async :as a]
            [clj-time.core :as time]
            [environ.core :refer [env]]
            [markov.chainz.rtm.api :as api]))

(def ^:dynamic *options* nil)
(def ^:dynamic *reconnect* nil)
(def ^:dynamic *websocket-stream* nil)

(defn send-to-websocket
  [data-json]
  (s/put! *websocket-stream* data-json))

(def message-id (atom 0))

(defn send-message
  [message]
  (-> message
      (assoc :id (swap! message-id inc))
      json/generate-string
      send-to-websocket))

(def pinging (atom false))
(def ping-loop (atom nil))
(def last-pong-time (atom nil))

(def ping-message {:type "ping"})

(defn start-ping []
  (swap! pinging (constantly true))
  (swap! last-pong-time (constantly (time/now)))
  (swap! ping-loop (constantly
                    (future
                      (loop []
                        (if (time/after? (time/now)
                                         (time/plus @last-pong-time (time/seconds 30)))
                          (future (*reconnect*))
                          (do (Thread/sleep 5000)
                              (send-message ping-message)
                              (when @pinging (recur)))))))))

(defn stop-ping []
  (swap! pinging (constantly false))
  (future-cancel @ping-loop))

(defn connect-websocket-stream
  [ws-url]
  @(http/websocket-client ws-url))

(defn handle-event
  [event]
  (let [event-type (:type event)]
    (case event-type
      "pong" (swap! last-pong-time (constantly (time/now))) ;; todo host callback for this message-id?
      "team_migration_started" (*reconnect*)
      "default")
    (when (and (*options* :log)
               (not= event-type "pong"))
      (println event))))

(defn event-json->event
  [event-json]
  (json/parse-string event-json true))

(defn start-real-time!
  [api-token set-team-state pass-event-to-rx reconnect options]
  (alter-var-root (var *reconnect*) (constantly reconnect))
  (alter-var-root (var *options*) (constantly options))
  (let [response-body @(api/rtm-start api-token)
        ws-url (:url response-body)
        ws-stream (connect-websocket-stream ws-url)]
    (set-team-state response-body)
    (alter-var-root (var *websocket-stream*) (constantly ws-stream)))
  (start-ping)
  (let [slack-event-stream (s/map event-json->event *websocket-stream*)
        conn-event-stream (s/stream 16)]
    (s/connect slack-event-stream conn-event-stream)
    (s/consume handle-event conn-event-stream)
    (s/consume pass-event-to-rx slack-event-stream)))

(defn stop-real-time!
  []
  (stop-ping)
  (when *websocket-stream*
    (s/close! *websocket-stream*)))
