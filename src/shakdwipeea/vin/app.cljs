(ns shakdwipeea.vin.app
  (:require [shakdwipeea.vin.three :as t]
            [shakdwipeea.vin.camera :as c]
            [clojure.core.async :as a :refer [go >! <!]]
            [ghostwheel.core :as g
             :refer [>defn >defn- >fdef => | <- ?]]
            [ghostwheel.tracer]))

(def π Math/PI)

(def camera {::t/object ::t/perspective-camera
             ::c/fov       90
             ::c/aspect    (t/aspect)
             ::c/near      0.1
             ::c/far       1000
             ::c/position  [0 1.8 10]
             ::c/look-at   [0  0  0]})

(def bulb {::t/object ::t/bulb
           :radius    1
           :width     32
           :height    32
           :position  [0 2 0]
           :color     :white
           :intensity 100})

;; (def room {:resource "elendil.glb"
;;            ::t/object ::t/gltf-model})

(def room {:resource "max/portalgun.3ds"
           :textures "max/textures/"
           ::t/object ::t/tds-model})

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
             :color :white
             :rotation-x (/ π 2)
             :width-segments 4
             :height-segments 4})

(defn main []
  (t/draw "canvas"
          {:camera camera
           :objects [bulb cube1 cube2 cube3 room]}))

(main)
