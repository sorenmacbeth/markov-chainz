(ns markov.chainz.rtm.api
  (:require [cheshire.core :as json]
            [aleph.http :as http]
            [clj-time.coerce :as coerce]
            [byte-streams :as bs]
            [manifold.deferred :as d]
            [clojure.java.io :as io]))

(def ^:const BASE-URL "https://slack.com/api")

(def emoji (load-string (slurp (io/resource "emoji.clj"))))

(defn time->ts [time]
  (-> time (coerce/to-long) (/ 1000) int str))

(defn api-request
  ([method-name]
   (api-request method-name {}))
  ([method-name params]
   (let [method-url-base (str BASE-URL "/" method-name)]
     (d/chain
       (http/post method-url-base {:connection-timeout 12e4
                                   :query-params params})
       :body
       bs/to-string
       #(json/parse-string % true)))))

(defn api-test
  [params]
  (api-request "api.test" params))

(defn rtm-start
  [api-token]
  (->> {:token api-token}
       (api-request "rtm.start")))

(defn chat-post-message
  [api-token channel text & params]
  (let [params-map (assoc (apply hash-map params) :channel channel :text text :token api-token)]
    (->> params-map
         (api-request "chat.postMessage"))))

(defn channels-set-topic
  [api-token channel-id topic]
  (->> {:token api-token :channel channel-id :topic topic}
       (api-request "channels.setTopic")))

(defn channels-list
  ([api-token]
   (channels-list api-token false))
  ([api-token exclude-archived]
   (->> {:token api-token :exclude_archived (if exclude-archived 1 0)}
        (api-request "channels.list")
        :channels)))

(defn im-open
  [api-token user-id]
  (->> {:token api-token :user user-id}
       (api-request "im.open")
       :channel
       :id))

(defn groups-list
  ([api-token]
   (channels-list api-token false))
  ([api-token exclude-archived]
   (->> {:token api-token :exclude_archived (if exclude-archived 1 0)}
        (api-request "groups.list")
        :groups)))

(defn im-list
  [api-token]
  (->> {:token api-token}
       (api-request "im.list")
       :ims))

(defn team-info
  [api-token]
  (->> {:token api-token}
       (api-request "team.info")
       :team))

(defn users-list
  [api-token]
  (->> {:token api-token}
       (api-request "users.list")
       :members))

(defn reactions-add
  [api-token emoji-name channel-id timestamp]
  (->> {:token api-token :name emoji-name :channel channel-id :timestamp timestamp}
       (api-request "reactions.add")))

(defn reactions-get
  [api-token channel-id timestamp full?]
  (->> {:token api-token :channel channel-id :timestamp timestamp :full full?}
       (api-request "reactions.get")))

(defn emoji-list
  [api-token]
  (->> {:token api-token}
       (api-request "emoji.list")
       :emoji))
