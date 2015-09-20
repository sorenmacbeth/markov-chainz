(ns markov.chainz.rtm.api
  (:require
    [cheshire.core :as json]
    [aleph.http :as http]
    [byte-streams :as bs]))

(def ^:const BASE-URL "https://slack.com/api")

(defn api-response
  "Takes a full http response map and returns the api response as a map."
  [http-response]
  (let [response-body-bytes (:body http-response)
        response-body-json (bs/to-string response-body-bytes)
        api-response (json/parse-string response-body-json true)]
    api-response))

(defn api-request
  ([method-name]
   (api-request method-name {}))
  ([method-name params]
   (let [method-url-base (str BASE-URL "/" method-name)]
     @(http/post method-url-base {:query-params params}))))

(defn api-test
  [params]
  (api-request "api.test" params))

(defn rtm-start
  [api-token]
  (->> {:token api-token}
       (api-request "rtm.start")
       api-response))

(defn chat-post-message
  [api-token channel text & params]
  (let [params-map (assoc (apply hash-map params) :channel channel :text text :token api-token)]
    (->> params-map
         (api-request "chat.postMessage")
         api-response)))

(defn channels-set-topic
  [api-token channel-id topic]
  (->> {:token api-token :channel channel-id :topic topic}
       (api-request "channels.setTopic")
       api-response))

(defn channels-list
  ([api-token]
   (channels-list api-token false))
  ([api-token exclude-archived]
  (->> {:token api-token :exclude_archived (if exclude-archived 1 0)}
       (api-request "channels.list")
       api-response
       :channels)))

(defn im-open
  [api-token user-id]
  (->> {:token api-token :user user-id}
       (api-request "im.open")
       api-response
       :channel
       :id))

(defn groups-list
  ([api-token]
   (channels-list api-token false))
  ([api-token exclude-archived]
   (->> {:token api-token :exclude_archived (if exclude-archived 1 0)}
        (api-request "groups.list")
        api-response
        :groups)))

(defn im-list
  [api-token]
  (->> {:token api-token}
       (api-request "im.list")
       api-response
       :ims))

(defn team-info
  [api-token]
  (->> {:token api-token}
       (api-request "team.info")
       api-response
       :team))

(defn users-list
  [api-token]
  (->> {:token api-token}
       (api-request "users.list")
       api-response
       :members))

(defn reactions-add
  [api-token emoji-name channel-id timestamp]
  (->> {:token api-token :name emoji-name :channel channel-id :timestamp timestamp}
       (api-request "reactions.add")
       api-response))

(defn reactions-get
  [api-token channel-id timestamp full?]
  (->> {:token api-token :channel channel-id :timestamp timestamp :full full?}
       (api-request "reactions.get")
       api-response))
