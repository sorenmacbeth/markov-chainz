(ns markov.chainz.rtm.receive
  (:require [markov.chainz.rtm.team :as state]))

(defn dispatch-handle-event [event handle-fn] (:type event))

(defmulti handle-event dispatch-handle-event)

(defmethod handle-event "message"
  [event handle-fn]
  (handle-fn event))

(defmethod handle-event "channel_joined"
  [event handle-fn]
  (state/channel-joined (:channel event))
  (handle-fn event))

(defmethod handle-event :default
  [event handle-fn]
  (handle-fn event))
