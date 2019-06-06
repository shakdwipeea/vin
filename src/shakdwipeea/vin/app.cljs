(ns shakdwipeea.vin.app
  (:require [shakdwipeea.vin.three :as t]
            [clojure.core.async :as a :refer [go >! <!]]))

(def π Math/PI)

(def camera {::t/object ::t/perspective-camera
             :fov       90
             :aspect    (t/aspect)
             :near      0.1
             :far       1000
             :position  [0 1.8 10]
             :look-at   [0  0  0]})

(def bulb {::t/object ::t/bulb
           :radius    1
           :width     32
           :height    32
           :position  [0 2 0]
           :color     :white
           :intensity 100})

(def room {:resource "elendil.glb"
           ::t/object ::t/gltf-model})

(def cube1 {::t/object ::t/cube
            :width 2
            :height 2
            :depth 2
            :position [0 2 2]
            :color :yellow})

(def cube2 {::t/object ::t/cube
            :width 2
            :height 2
            :depth 2
            :position [5 2 0]
            :color :red})

(def cube3 {::t/object ::t/cube
            :width 2
            :height 2
            :depth 2
            :position [-5 2 0]
            :color :green})

(def ground {::t/object ::t/plane
             :width  200
             :height 200
             :color :brown
             :rotation-x (/ π 2)
             :width-segments 4
             :height-segments 4})

(defn main []
  (t/draw "canvas"
          {:camera camera
           :objects [ground cube1 cube2 cube3]}))

(main)
