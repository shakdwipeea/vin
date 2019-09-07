(ns shakdwipeea.vin.camera
  (:require ["three" :as t]
            ["three/examples/jsm/controls/PointerLockControls" :as p]
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
                          (when-not (nil? ev)
                            (a/go (a/>! cb-chan ev))))
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
  [{:keys [camera clock position mouse-controls] :as game-state} m]
  ;; (aset (.getObject mouse-controls)
  ;;       "position"
  ;;       "y"
  ;;       (+ (.. (.getObject mouse-controls) -position -y)
  ;;          (* (.getDelta clock) )))
  (update game-state
          ::position
          (translate (.getDelta clock) m)))


(defn describe-move
  "returns a map of type camera where move-start
   and move-stop refer to the direction in which to start or stop.
   Provide direction keyword in start and stop"
  [{:keys [start stop]}]
  (cond->   {:type :camera}
    (some? start) (merge {:move-start start})
    (some? stop)  (merge {:move-stop  stop})))

(defn keyboard-xf
  "returns a transducer whose behaviour is to
   convert the incoming keycode to a direction
   and then to a map which as returned by decribe-move
   in the key defined by k
   "
  [k]
  (comp (map keycode->direction)
        (map #(describe-move {k %}))))


(defn id->dom [id] (.getElementById js/document id))


;; todo horrible fn
(defn patch-pointer-lock!
  "cross browser support for pointer lock functions
   equivalent js is
   ///
   canvas.requestPointerLock = canvas.requestPointerLock ||
   canvas.mozRequestPointerLock;
  
   document.exitPointerLock = document.exitPointerLock ||
   document.mozExitPointerLock;
  "
  [dom-id]
  (let [element (id->dom dom-id)]
    (aset element
          "requestPointerLock"
          (or (.-requestPointerLock element)
              (.-mozRequestPointerLock element)))
    (aset js/document
          "exitPointerlock"
          (or (.-exitPointerLock js/document)
              (.-mozExitPointerLock js/document)))))


(defn add-camera+
  "returns a channel where updates required to affect camera will
   be provided"
  [{c ::controls} canvas-dom-id]
  (let [keydown-chan (add-event-listener+ :keydown (keyboard-xf :start))
        keyup-chan (add-event-listener+ :keyup (keyboard-xf :stop))
        _          (add-event-listener+ :click (map (fn [e]
                                                      (.lock c)
                                                      :a)))]
    (helpers/create-mixer+ [keyup-chan keydown-chan])))


(s/def ::camera #(instance? t/PerspectiveCamera %))
(s/def ::position ::m/vector)
(s/def ::look-at  ::m/vector)
(s/def ::fov int?)
(s/def ::aspect number?)
(s/def ::far number?)
(s/def ::near number?)

(s/def ::camera-props (s/keys :req [::position ::look-at ::fov
                                    ::aspect   ::far
                                    ::near]))


(s/def ::camera-map (s/keys :req [::camera ::position ::look-at]))

(defn create-vector3 [[x y z]] (t/Vector3. x y z))

(defn make-camera!
  "create a new camera"
  [canvas-dom-id 
   {:keys [::fov ::aspect ::near ::far ::position ::look-at]}]
  (let [camera (t/PerspectiveCamera. fov aspect near far)]
    (helpers/set-position! camera position)
    (.lookAt camera (create-vector3 look-at))
    {::camera   camera
     ::controls (p/PointerLockControls. camera)
     ::position position}))
