(ns shakdwipeea.vin.app
  (:require [shakdwipeea.vin.three :as t]
            [clojure.core.async :as a :refer [go >! <!]]))

(def bulb {::t/object ::t/bulb
           :radius    1
           :width     32
           :height    32
           :position  [0 2 0]
           :color     0xffffee
           :intensity 100})

(def room {:resource "elendil.glb"
           ::t/object ::t/gltf-model})

(defn main []
  (t/draw "canvas"
          [bulb room]))

(main)
