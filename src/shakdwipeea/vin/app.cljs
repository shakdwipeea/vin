(ns shakdwipeea.vin.app
  (:require ["three" :as t]
            ["three/examples/jsm/controls/OrbitControls" :as orbit]
            ["./draco_loader" :as d]
            ["three-gltf-loader" :as gltf]
            [clojure.core.async :as a :refer [go >! <!]]))

;; js interop

(def mouse (atom {:x 0 :y 0}))

(defn width [] (.-innerWidth js/window))
(defn height [] (.-innerHeight js/window))

;; scene

(defn create-scene [& scenes]
  (let [scene (t/Scene.)]
    ;; (aset scene "background" (doto (t/Color.)
    ;;                            (.setHSL 0.6 0 1)))
    (when (seq? scenes) (doall (map #(.add scene %) scenes)))
    scene))

;; renderer

(defn create-renderer []
  (doto (t/WebGLRenderer.)
    (.setPixelRatio (.-devicePixelRatio js/window))
    (.setSize (width) (height))
    (aset "toneMapping" t/ReinhardToneMapping)))

;; lights

(defn create-ambient-light [color intensity]
  (t/AmbientLight. color intensity))

(defn create-point-light [{[x y z] :position
                           :keys [color intensity distance]
                           :or {distance 100}
                           :as p}]
  (println x y z)
  (let [light (t/PointLight. color intensity distance 2)]
    (.set (.-position light) x y z)
    light))

(defn create-directional-light [{[x y z] :position
                                 :keys [position color intensity]}]
  (let [light (t/DirectionalLight. color intensity)]
    (.set (.-position light) x y z)
    light))

;; geometery primitives

(defn create-box [{:keys [width height depth]}]
  (t/BoxGeometry. width height depth))

(defn create-basic-material [color]
  (t/MeshBasicMaterial. #js {:color color}))

(defn create-normal-material []
  (t/MeshNormalMaterial.))

(defn create-mesh-phong-material [{:keys [color dithering]
                                   :or {color 0xffffee
                                        dithering true}
                                   :as opts}]
  (t/MeshPhongMaterial. (clj->js opts)))

(defn create-standard-material
  [& {:keys [emissive emissive-intensity color]
      :or   {emissive 0xffffee
             emissive-intensity 10
             color 0xffffee}}]
  (t/MeshStandardMaterial. (clj->js {:emissive emissive
                                     :emissiveIntensity emissive-intensity
                                     :color color})))

(defn create-mesh [g m & {position :position}]
  (let [m (t/Mesh. g m)]
    (when-let [[x y z] position]
      (-> m
          .-position
          (.set x y z)))
    m))

(defn create-cube [{:keys [width height depth] :as params}]
  (create-mesh (create-box params)
               (create-basic-material 0x00234c)))

(defn create-sphere [{:keys [radius width height]}]
  (t/SphereGeometry. radius width height))

(defn create-bulb [{[x y z] :position
                    :keys [radius width height color intensity]
                    :as params}]
  (doto (create-point-light params)
    (.add (create-mesh (create-sphere params)
                       (create-standard-material)))
    (aset "castShadow" true)))

(defn cb->chan
  ([] (cb->chan (a/chan 1)))
  ([ch] (fn [val] (a/go (a/>! ch val)))))


;; load models
(defn load-gltf-model
  "returns a channel which will have the loaded model"
  [model]
  (let [ch (a/chan 1)
        loader (gltf.)]
    (.setDecoderPath t/DRACOLoader "/draco/")
    (.getDecoderModule t/DRACOLoader)
    (doto loader
      (.setDRACOLoader (t/DRACOLoader.))
      (.load model (cb->chan ch)))
    ch))

;; camera
(defn create-camera [] (t/PerspectiveCamera. 70
                                             (/ (width) (height))
                                             0.01
                                             1000))

(defn add-light-to-camera! [camera light]
  (.add camera light))

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
    (39 68) :right)
  :i-dont-know)

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

(defn load-texture [url]
  (.load (t/TextureLoader.) url))

(defn add-scene-in-state [game-state scene]
  (update game-state :scene (fn [s] (.add (or s
                                             (create-scene)) scene) )))

(defn game-loop [game-state chans]
  (a/go (loop [g game-state]
          (let [[v _] (a/alts! chans)
                g1  (cond-> g
                      (= (v :type) :mesh)
                      (add-scene-in-state (v :mesh)))]
            (when (not= (v :type) :frame) (println v))
            (.render renderer
                     (:scene g1)
                     (:camera g1))
            (recur g1)))))

(defn add-scene
  "puts scene in scene chan"
  [scene-chan scene]
  (a/go (a/>! scene-chan scene)))

(defn frame-loop [frame-chan]
  (.requestAnimationFrame js/window (fn []
                                      (go (>! frame-chan
                                              {:type :frame}))
                                      (frame-loop frame-chan))))


(defn add-scene! [scene-chan mesh]
  (go  (>! scene-chan
           {:type :mesh
            :mesh mesh})))

(defn render-canvas [renderer dom-id]
  (let [canvas (.getElementById js/document dom-id)
        camera (create-camera)
        keydown-chan (a/chan 20 (map keycode->direction))
        frame-chan (a/chan 20)
        scene-chan (a/chan 10)]
    (if (.hasChildNodes canvas)
      (.replaceChild canvas
                     (.-domElement renderer)
                     (aget (.-childNodes canvas) 0))
      (.appendChild canvas (.-domElement renderer)))
    (println camera)
    (aset camera "position" "z" 30)
    (doto (orbit/OrbitControls. camera)
      #(.set camera '(0 -1 0))
      .update)
    ;; (add-event-listener "keyup" keydown-chan)
    (doto scene-chan

      (add-scene! (create-bulb {:radius    1
                                :width     32
                                :height    32
                                :position  [0 2 0]
                                :color     0xffffee
                                :intensity 100}))

      (add-scene! (t/HemisphereLight. 0xffffbb 0x080820 1))


      ;; (add-scene! (create-mesh (create-box {:width 3
      ;;                                       :height 1
      ;;                                       :depth 2})
      ;;                          (create-mesh-phong-material {})
      ;;                          {:position [0 -1 0]}))
      )

    ;; (a/pipeline 1
    ;;             scene-chan
    ;;             (map (fn [m]
    ;;                    {:type :mesh
    ;;                     :mesh (-> m .-scene)}))
    ;;             (load-gltf-model "elendil.glb"))
    (game-loop {:camera camera
                :start-time (js/Date.)}
               [frame-chan scene-chan])
    (frame-loop frame-chan)))

(defn main []
  (println "Well.. Hello there!!!!")
  (render-canvas renderer "canvas"))

(main)
