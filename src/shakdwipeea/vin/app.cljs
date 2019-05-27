(ns shakdwipeea.vin.app
  (:require ["three" :as t]
            [clojure.core.async :as a]))

;; js interop

(def mouse (atom {:x 0 :y 0}))

(defn width [] (.-innerWidth js/window))
(defn height [] (.-innerHeight js/window))

;; scene

(defn create-scene [objs]
  (let [scene (t/Scene.)]
    (doall (map #(.add scene %) objs))
    scene))

;; renderer

(defn create-renderer []
  (doto (t/WebGLRenderer.)
    (.setPixelRatio (.-devicePixelRatio js/window))
    (.setSize (width) (height))))

;; lights

(defn create-ambient-light [color intensity]
  (t/AmbientLight. color intensity))

(defn create-point-light [color intensity {:keys [x y z]}]
  (let [light (t/PointLight. color intensity)]
    (.set (.-position light) x y z)
    light))

;; geometery primitives

(defn create-box [w h d]
  (t/BoxGeometry. w h d))

(defn create-basic-material [color]
  (t/MeshBasicMaterial. #js {:color color}))

(defn create-normal-material []
  (t/MeshNormalMaterial.))

(defn create-mesh [g m] (t/Mesh. g m))

(defn create-cube [w h d]
  (create-mesh (create-box w h d)
               (create-basic-material 0x00234c)))


;; load models
;; (defn load-gltf-model [model]
;;   (.setDecoderPath t/DRACOLoader "/draco")
;;   (.getDecoderModule t/DRACOLoader)
;;   (doto (t/GLTFLoader.)
;;     (.setDRACOLoader t/DRACOLoader)
;;     (.load loader
;;            model
;;            )))

;; camera

(defn create-camera [] (t/PerspectiveCamera. 70
                                             (/ (width) (height))
                                             0.01
                                             10))

;; three js

(def game-state (atom {}))
(def renderer (create-renderer))

(defn add-event-listener [event event-chan]
  (.addEventListener js/document event (fn [event]
                                         (println (.-keyCode event))
                                         (a/go (a/>! event-chan
                                                     event))) false))

(defn keycode->direction [event]
  (case (.-keyCode event)
    ;; up w
    (38 87) :forward
    ;; left a
    (37 65) :left
    ;; down s
    (40 83) :back
    ;; right d
    (39 68) :right))

;; (defn register-keys!
;;   (add-event-listener 'keydown
;;                       )
;;   )

(defn calculate-fps [game-state]
  (* (/ (:frame game-state)
        (- (.getTime (js/Date.))
           (.getTime
            (:start-time game-state))))
     1000))


(defn game-loop [game-state chans]
  (a/go (loop [g game-state]
          (let [[v _] (a/alts! chans)]
            (println v)
            (.render renderer
                     (:scene  game-state)
                     (:camera game-state))
            (recur g)))))


(defn render-canvas [renderer dom-id]
  (let [canvas (.getElementById js/document dom-id)
        camera (create-camera)
        keydown-chan (a/chan 20 (map keycode->direction))
        frame-chan (a/chan 20)]
    (if (.hasChildNodes canvas)
      (.replaceChild canvas
                     (.-domElement renderer)
                     (aget (.-childNodes canvas) 0))
      (.appendChild canvas (.-domElement renderer)))
    (aset camera "position" "z" 1)
    (add-event-listener "keyup" keydown-chan)
    (game-loop {:camera camera
                :start-time (js/Date.)
                :scene (create-scene (list (create-cube 0.2 0.2 0.2)))}
               [keydown-chan])))

(defn main []
  (println "Well.. Hello there!!!!")
  (render-canvas renderer "canvas"))

(main)
