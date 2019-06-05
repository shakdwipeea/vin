(ns shakdwipeea.vin.app
  (:require [shakdwipeea.vin.three :as t]
            [clojure.core.async :as a :refer [go >! <!]]))

(def π Math/PI)

(def camera {::t/object ::t/perspective-camera
             :fov       90
             :aspect    (t/aspect)
             :near      0.1
             :far       1000
             :position  [0 1.8 -5]
             :look-at   [0 1.8  0]})

(def bulb {::t/object ::t/bulb
           :radius    1
           :width     32
           :height    32
           :position  [0 2 0]
           :color     0xffffee
           :intensity 100})

(def room {:resource "elendil.glb"
           ::t/object ::t/gltf-model})

(def plane {::t/object ::t/plane
            :width  2000
            :height 2000
            :color 0xffffee
            :rotation-x (/ π 2)
            :width-segments 8
            :height-segments 8})

(defn main []
  (t/draw "canvas"
          {:camera camera
           :objects [plane]}))

(main)
