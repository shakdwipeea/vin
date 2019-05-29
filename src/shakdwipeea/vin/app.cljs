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

(defn create-scene [scenes]
  (let [scene (t/Scene.)]
    (doall (map #(.add scene %) scenes))
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

(defn create-sphere [r w h] (t/SphereBufferGeometry. r w h))

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
  (update game-state :scene conj scene))

(defn game-loop [game-state chans]
  (a/go (loop [g game-state]
          (let [[v _] (a/alts! chans)
                g1  (cond-> g
                      (= (v :type) :mesh)
                      (add-scene-in-state (v :mesh)))]
            
            (.render renderer
                     (create-scene (:scene g1))
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
    (aset camera "position" "z" 3)
    (doto (orbit/OrbitControls. camera)
      #(.set (.-target %) '(0 -0.2 -0.2))
      .update)
    ;; (add-event-listener "keyup" keydown-chan)

    (go  (>! scene-chan
             {:type :mesh
              :mesh (let [l (t/PointLight. 0xffffff 2 50)]
                      (.add l
                            (create-mesh (create-sphere 0.5 1 1)
                                         (create-basic-material 0xff0040)))
                      (.set (.-position l) 4 4 4)
                      l)}))

    ;; (go (>! scene-chan {:type :scene
    ;;                     :scene (t/AmbientLight. 0xcccccc 0.4)}))

    ;; (go (>! scene-chan {:type :mesh
    ;;                     :mesh (create-cube 0.2 0.2 0.2)}))
    (def t (load-texture "texture.jpg"))
    (a/pipeline 1
                scene-chan
                (map (fn [m]
                       ;; (.traverse (.-scene m)
                       ;;            (fn [child]
                       ;;              (when (.-isMesh child)
                       ;;                (aset child "material" "envmap"
                       ;;                      t))))
                       ;; (aset (.-scene m) "background" t)
                       {:type :mesh
                        :mesh (-> m .-scene)}))
                (load-gltf-model "glTF/Duck.gltf"))
    (game-loop {:camera camera
                :start-time (js/Date.)}
               [frame-chan scene-chan])
    (frame-loop frame-chan)))

(defn main []
  (println "Well.. Hello there!!!!")
  (render-canvas renderer "canvas"))

(main)
