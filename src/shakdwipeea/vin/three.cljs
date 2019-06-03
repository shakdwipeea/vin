(ns shakdwipeea.vin.three
  (:require ["three" :as t]
            ["three-gltf-loader" :as gltf]
            ["./draco_loader" :as d]
            [clojure.core.reducers :as r]
            [clojure.core.async :as a :refer [go >! <!]]))

;; core async helpers

(defn chan?
  "is ch a channel"
  [ch]
  (satisfies? cljs.core.async.impl.protocols/WritePort ch))

(defn cb->chan+
  ([] (cb->chan+ (a/chan 1)))
  ([ch] (fn [val] (a/go (a/>! ch val)))))

(defn transform-chan
  "takes a channel and returns a channel with applied transducer xf"
  [ch xf]
  (let [dest-chan (a/chan 1)]
    (a/pipeline 1 dest-chan xf ch)
    dest-chan))

(defn to-channel+
  "takes a value or channel and returns a channel with f applied to
   the value flowing through"
  [val-or-ch f]
  (if (chan? val-or-ch)
    (transform-chan val-or-ch (map f))
    (a/to-chan (list (f val-or-ch)))))

;; (def test-chan (a/to-chan '(12 3)))
;; (chan? test-chan)
;; (chan? '( 12))


(defn width [] (.-innerWidth js/window))
(defn height [] (.-innerHeight js/window))


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


;; (when (some? background-color)
;;   (aset scene "background" background-color))
(defn create-scene [] (t/Scene.))

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


;; materials

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



;; geometery primitives

(defn create-box [{:keys [width height depth]}]
  (t/BoxGeometry. width height depth))


;; mesh shapes

(defmulti map->mesh #(-> % ::object))

(defn create-mesh [{:keys [geometry material  position]}]
  (let [m (t/Mesh. geometry material)]
    (when-let [[x y z] position]
      (-> m
          .-position
          (.set x y z)))
    m))


(defn create-cube [{:keys [width height depth] :as params}]
  (create-mesh {:geometry (create-box params)
                :material (create-basic-material 0x00234c)}))


(defn create-sphere-geometry [{:keys [radius width height]}]
  (t/SphereGeometry. radius width height))


(defn create-bulb [{[x y z] :position
                    :keys [radius width height color intensity]
                    :as params}]
  (doto (create-point-light params)
    (.add (create-mesh {:geometry (create-sphere-geometry params)
                        :material (create-standard-material)}))
    (aset "castShadow" true)))

(defmethod map->mesh ::bulb [m] (create-bulb m))


;; load models

(defn load-gltf-model+
  "returns a channel which will have the loaded model"
  [{res :resource}]
  (let [ch (a/chan 1 (map (fn [model] (.-scene model))))
        loader (gltf.)]
    (.setDecoderPath t/DRACOLoader "/draco/")
    (.getDecoderModule t/DRACOLoader)
    (doto loader
      (.setDRACOLoader (t/DRACOLoader.))
      (.load res (cb->chan+ ch)))
    ch))

(defmethod map->mesh ::gltf-model [m] (load-gltf-model+ m))

;; camera

(defn create-camera [] (t/PerspectiveCamera. 70
                                             (/ (width) (height))
                                             0.01
                                             1000))

(defn add-light-to-camera! [camera light]
  (.add camera light))

;; three js

(def game-state (atom {}))

(defn add-event-listener [event event-chan]
  (.addEventListener js/document
                     event
                     (fn [event]
                       (println (.-keyCode event))
                       (a/go (a/>! event-chan
                                   event)))
                     false))


(defn add-scene-in-state [game-state scene]
  (println "adding scene " scene)
  (update game-state :scene (fn [s]
                              (.add s scene)
                              s)))


(defn perform-render! [{:keys [scene camera renderer] :as game-state}]
  (.render renderer scene camera))


(defn get-next-frame
  [current-game {type :type :as update-map}]
  (cond-> current-game
    (= type :mesh)  (add-scene-in-state (update-map :mesh))
    (= type :frame) (assoc :render? true))) 


(defn game-state-transducer [game-step-fn initial-state]
  (fn [rf]
    (let [game-state (volatile! initial-state)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input] (->> input
                             (game-step-fn @game-state)
                             (vreset! game-state)
                             (rf result)))))))

(defn get-next-frame+
  "starting from game-state process events coming in from chans
   and returns a channel which will contain updated game state
   whenever we get a value in any of the chans"
  [game-state chans]
  (let [game-chan (a/chan 1 (game-state-transducer get-next-frame
                                                   game-state))
        game-mixer (a/mix game-chan)]
    (doseq [ch chans] (a/admix game-mixer ch))
    game-chan))


(defn game-loop [initial-game-state chans]
  (let [game-frame-chan (get-next-frame+ initial-game-state chans)]
    (go (loop []
          (let [{:keys [render?] :as o} (<! game-frame-chan)]
            
            (when render?
              (perform-render! o)))
          (recur)))))


(defn frame-loop [frame-chan]
  (.requestAnimationFrame js/window (fn []
                                      (go (>! frame-chan
                                              {:type :frame}))
                                      (frame-loop frame-chan))))


(defn replace-canvas [renderer dom-id]
  (let [canvas (.getElementById js/document dom-id)]
    (if (.hasChildNodes canvas)
      (.replaceChild canvas
                     (.-domElement renderer)
                     (aget (.-childNodes canvas) 0))
      (.appendChild canvas (.-domElement renderer)))
    renderer))

;; (doto scene-chan

;;   (add-scene! (create-bulb {:radius    1
;;                             :width     32
;;                             :height    32
;;                             :position  [0 2 0]
;;                             :color     0xffffee
;;                             :intensity 100}))

;;   (add-scene! (t/HemisphereLight. 0xffffbb 0x080820 1))

;;   (add-scene! (create-mesh (create-box {:width 3
;;                                         :height 1
;;                                         :depth 2})
;;                            (create-mesh-phong-material {})
;;                            {:position [0 -1 0]})))

;; (a/pipeline 1
;;             scene-chan
;;             (map (fn [m]
;;                    {:type :mesh
;;                     :mesh (-> m .-scene)}))
;;             (load-gltf-model "elendil.glb"))

(defn mesh->map [mesh]
  {:type :mesh
   :mesh mesh})



(defn draw-scenes+ [renderer scene]
  (a/merge (doall (map #(-> %
                            map->mesh
                            (to-channel+ mesh->map))
                       scene))))


(defn render-scene [renderer scene]
  (let [camera (create-camera)
        keydown-chan (a/chan 20 (map keycode->direction))
        frame-chan (a/chan 20)
        scene-chan (a/chan 10)]
    (aset camera "position" "z" 30)
    (game-loop {:renderer renderer
                :camera camera
                :scene (t/Scene.)}
               [scene-chan frame-chan])
    (frame-loop frame-chan)
    (a/pipe (draw-scenes+ renderer scene) scene-chan)))


(defn draw [canvas-dom-id scene]
  (-> (create-renderer)
      (replace-canvas canvas-dom-id)
      (render-scene scene)))


;; (render-canvas renderer "canvas")
