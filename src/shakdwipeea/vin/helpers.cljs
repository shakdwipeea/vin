(ns shakdwipeea.vin.helpers
  (:require [shakdwipeea.vin.math :as m]
            [clojure.core.async :as a :refer [>! <! go]]
            [ghostwheel.core :as g
             :refer [>defn >defn- >fdef => | <- ?]]))

(>defn set-position!
       "set position (mutate) of an object and return the updated 
        object"
       [obj [x y z]]
       [some? ::m/vector => some?]
       (.set (.-position obj) x y z)
       obj)

(defn swap-position! [obj f]
  (f (.-position obj))
  obj)

(defn mix-chans
  [chans output-chan]
  (let [mixer (a/mix output-chan)]
    (doseq [ch chans]
      (a/admix mixer ch))
    output-chan))

(defn create-mixer+
  ([chans]
   (create-mixer+ chans identity))
  ([chans xf]
   (mix-chans chans (a/chan 10 xf))))
