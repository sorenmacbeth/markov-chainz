(ns markov.chainz.rtm.receive
  (:require [markov.chainz.rtm.team :as state]))

(defn dispatch-handle-event [event] (:type event))

(defmulti handle-event dispatch-handle-event)

(defmethod handle-event "message"
  [event]
  event)

(defmethod handle-event "channel_joined"
  [event]
  (state/channel-joined (:channel event))
  event)

(defmethod handle-event :default
  [event]
  event)
