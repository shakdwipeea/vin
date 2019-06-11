(ns shakdwipeea.vin.helpers
  (:require [shakdwipeea.vin.math :as m]
            [ghostwheel.core :as g
             :refer [>defn >defn- >fdef => | <- ?]]))

(>defn set-position!
       "set position of an object"
       [obj [x y z]]
       [some? ::m/vector => some?]
       (.set (.-position obj) x y z)
       obj)

(defn swap-position! [obj f]
  (f (.-position obj))
  obj)
