(ns shakdwipeea.vin.camera
  (:require ["three" :as t]
            [clojure.core.async :as a :refer [go <! >!]]
            [shakdwipeea.vin.helpers :as helpers]
            [shakdwipeea.vin.math :as m]
            [clojure.spec.alpha :as s]
            [ghostwheel.core :as g
             :refer [>defn >defn- >fdef => | <- ?]]))

(defn add-event-listener+
  ([event] (add-event-listener+ event identity))
  ([event xf]
   (let [cb-chan (a/chan (a/sliding-buffer 10) xf)]
     (.addEventListener js/document
                        (name event)
                        (fn [ev]
                          (a/go (a/>! cb-chan ev)))
                        false)
     cb-chan)))

(defn keycode->direction [event]
  (let [code (.-keyCode event)]
    (case code
      ;; up w
      (38 87) :forward
      ;; left a
      (37 65) :left
      ;; down s
      (40 83) :back
      ;; right d
      (39 68) :right
      :unknown-key)))

(def keycode-xf (map keycode->direction))

(def velocity 2)

(defn move-position [pos move-start δt]
  (let [δ (* velocity δt)]
    (case move-start
      :forward (m/move-z - δ pos) 
      :right   (m/move-x + δ pos)
      :left    (m/move-x - δ pos)
      :back    (m/move-z + δ pos)
      pos)))

(defn translate [δt {:keys [move-start move-stop]}]
  (fn [pos]
    (cond-> pos
      (some? move-start) (move-position move-start δt))))


(defn update-camera
  "consumes the side effect described in m and returns the updated
   game state"
  [{:keys [camera clock position] :as game-state} m]
  (update game-state
          ::position
          (translate (.getDelta clock) m)))


(defn describe-move!
  "puts a map of type camera in the camera-ch channel where move-start
   and move-stop refer to the direction in which to start or stop.
   Provide direction keyword in start and stop"
  [camera-ch {:keys [start stop]}]
  (go (>! camera-ch
          (cond->   {:type :camera}
            (some? start) (merge {:move-start start})
            (some? stop)  (merge {:move-stop  stop})))))


(defn add-camera+
  "returns a channel where updates required to affect camera will
   be provided"
  []
  (let [camera-chan (a/chan 10)
        keydown-chan (add-event-listener+ :keydown keycode-xf)
        keyup-chan (add-event-listener+ :keyup keycode-xf)]
    (a/go (loop []
            (a/alt! keydown-chan ([direction]
                                  (describe-move! camera-chan
                                                  {:start direction}))
                    keyup-chan  ([direction]
                                 (describe-move! camera-chan
                                                 {:stop direction})))
            (recur)))
    camera-chan))

(s/def ::camera #(instance? t/PerspectiveCamera %))
(s/def ::position ::m/vector)
(s/def ::look-at  ::m/vector)
(s/def ::fov int?)
(s/def ::aspect number?)
(s/def ::far number?)
(s/def ::near number?)

(s/def ::camera-props (s/keys :req [::position ::look-at
                                    ::fov    ::aspect   ::far
                                    ::near]))


(s/def ::camera-map (s/keys :req [::camera ::position ::look-at]))

(defn create-vector3 [[x y z]] (t/Vector3. x y z))

(>defn make-camera!
       "create a new camera"
       [{:keys [::fov ::aspect ::near ::far ::position ::look-at]}]
       [::camera-props | #(< near far)
        => ::camera-map]
       (let [camera (t/PerspectiveCamera. fov aspect near far)]
         (helpers/set-position! camera position)
         (.lookAt camera (create-vector3 look-at))
         {::camera   camera
          ::position position}))
