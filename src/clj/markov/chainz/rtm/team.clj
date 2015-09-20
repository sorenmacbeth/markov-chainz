(ns markov.chainz.rtm.team)

(def state (atom nil))

(defn team-state!
  [state-map]
  (swap! state (constantly state-map)))

(defn get-team-state [] @state)

(defn self
  []
  (:self @state))

(defn self-id
  []
  (:id (self)))

(defn id->user
  [id]
  (->> @state
       :users
       (filter #(= (:id %) id))
       first))

(defn id->name
  [id]
  (->> (id->user id)
       :name))

(defn name->id
  [name]
  (->> @state
       :users
       (filter #(= (:name %) name))
       first
       :id))

(defn id->channel
  [id]
  (->> @state
       :channels
       (filter #(= (:id %) id))
       first))

(defn name->channel
  [name]
  (->> @state
       :channels
       (filter #(= (:name %) name))
       first))

(defn bot?
  [user-id]
  (let [user (id->user user-id)]
    (:is_bot user)))

(defn dm?
  [channel-id]
  (.startsWith channel-id "D"))

(defn user-id->dm-id
  [user-id]
  (->> @state
       :ims
       (filter #(= (:user %) user-id))
       first
       :id))

(defn channel-joined
  [channel]
  (swap! state #(assoc-in % [:channels] (conj (:channels %) channel))))
