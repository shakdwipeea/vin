(ns shakdwipeea.vin.three
  (:require ["three" :as t]
            ["three-gltf-loader" :as gltf]
            ["./draco_loader" :as d]
            ["three/examples/jsm/controls/FirstPersonControls" :as fp]
            [clojure.core.reducers :as r]
            [clojure.core.async :as a :refer [go >! <! chan]]
            [shakdwipeea.vin.camera :as c]
            [shakdwipeea.vin.math :refer [add-vector3]]
            [shakdwipeea.vin.helpers :as h]))

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

(defn aspect [] (/ (width) (height)))


(defn create-scene []
  (doto (t/Scene.)
    ;; (aset "background" (t/Color. "skyblue"))
    ))

;; renderer

(defn create-renderer []
  (doto (t/WebGLRenderer. #js {:anitalias true})
    (.setPixelRatio (.-devicePixelRatio js/window))
    (.setSize (width) (height))
    (aset "toneMapping" t/ReinhardToneMapping)))

;; lights

(defn create-ambient-light [color intensity]
  (t/AmbientLight. color intensity))


(defn create-point-light [{:keys [color intensity distance position]
                           :or {distance 100}
                           :as p}]
  (-> (t/PointLight. color intensity distance 2)
      (h/set-position!  position)))


(defn create-directional-light [{[x y z] :position
                                 :keys [position color intensity]}]
  (-> (t/DirectionalLight. color intensity)
      (h/set-position!  position)))


;; materials

(defn color->js [color]
  (t/Color. (cond->  color
              (keyword? color) name)))

(defn map->js [m]
  (clj->js (update m :color color->js)))


(defn create-basic-material [{:keys [color side]
                              :or {side t/FrontSide}
                              :as p}]
  (t/MeshBasicMaterial. (map->js p)))


(defn create-normal-material []
  (t/MeshNormalMaterial.))


(defn create-mesh-phong-material [{:keys [color dithering]
                                   :or {color 0xffffee
                                        dithering true}
                                   :as opts}]
  (println "[hong material]" opts)
  (t/MeshPhongMaterial. (map->js opts)))


(defn create-standard-material
  [& {:keys [emissive emissive-intensity color]
      :or   {emissive 0xffffee
             emissive-intensity 10
             color 0xffffee}}]
  (t/MeshStandardMaterial. (clj->js {:emissive emissive
                                     :emissiveIntensity emissive-intensity
                                     :color color})))



;; geometery primitives

(defn create-box [{:keys [width height depth] :as o}]
  (println o)
  (t/CubeGeometry. width height depth))




;; mesh shapes

(defmulti map->mesh #(-> % ::object))

(defn create-mesh [{:keys [geometry material  position]}]
  (let [m (t/Mesh. geometry material)]
    (when-let [[x y z] position]
      (do (println "setting position " position)
          (-> m
              .-position
              (.set x y z))))
    m))


(defn create-cube [{:keys [width height depth position] :as params}]
  (create-mesh {:geometry (create-box params)
                :material (create-basic-material params)
                :position position}))

(defmethod map->mesh ::cube [m] (create-cube m))

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


(defn create-plane-geometry [{:keys [width height width-segments
                                     height-segments]
                              :or {width-segments 1
                                   height-segments 1}}]
  (t/PlaneGeometry. width height width-segments height-segments))


(defn create-plane [{:keys [color] :as m}]
  (let [mesh (create-mesh {:geometry (create-plane-geometry m)
                           :material (create-basic-material (merge m
                                                                   {:side t/DoubleSide}))})]
    mesh))

(defmethod map->mesh ::plane [m] (create-plane m))


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

(defn setup-fps-controls
  ([camera] (setup-fps-controls camera {:look-speed 0.4
                                        :movement-speed 10
                                        :no-fly false
                                        :look-vetical true
                                        :constrain-vertical true
                                        :vertical-min 1.0
                                        :vertical-max 2.0
                                        :lon -170
                                        :lat 100}))
  ([camera {:keys [look-speed movement-speed no-fly look-vertical
                   constrain-vertical vertical-min vertical-max lon
                   lat]}]
   (doto (fp/FirstPersonControls. camera) 
     (aset "lookSpeed" look-speed)
     (aset "movementSpeed" movement-speed)
     (aset "noFly" no-fly)
     (aset "lookVertical" look-vertical)
     (aset "constrainVertical" constrain-vertical)
     (aset "verticalMin" vertical-min)
     (aset "verticalMax" vertical-max)
     (aset "lon" lon)
     (aset "lat" lat))))

(defn add-light-to-camera! [camera light]
  (.add camera light))

;; three js


(defn add-scene-in-state [game-state scene]
  (println "adding scene " scene)
  (update game-state :scene (fn [s]
                              (.add s scene)
                              s)))


(defn perform-render! [{:keys [::c/position ::c/camera
                               scene renderer clock]
                        :as game-state}]
  ;; (h/set-position! camera position)
  (.render renderer scene camera))


(defn get-next-frame
  [current-game {type :type :as update-map}]
  (cond-> current-game
    (= type :mesh)   (add-scene-in-state (:mesh update-map))
    (= type :frame)  (assoc :render? true)
    (.-isLocked (::c/controls current-game)) (c/update-camera update-map))) 


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
  (->> game-state
       (game-state-transducer get-next-frame)
       (h/create-mixer+ chans)))


(defn game-loop [initial-game-state chans]
  (let [game-frame-chan (get-next-frame+ initial-game-state chans)]
    (go (loop []
          (let [{:keys [render?] :as o} (<! game-frame-chan)]
            ;; (when render?)
            (perform-render! o))
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


(defn mesh->map [mesh]
  {:type :mesh
   :mesh mesh})

(defn transform-mesh [mesh {:keys [rotation-x]}]
  (when (some? rotation-x)
    (aset mesh "rotation" "x"
          (+ (.. mesh -rotation -x)
             rotation-x)))
  mesh)

(defn draw-scenes+ [renderer scene]
  (a/merge (doall (map (fn [m]
                         (-> m
                             map->mesh
                             (transform-mesh m)
                             (to-channel+ mesh->map)))
                       scene))))


(defn render-scene [renderer canvas-dom-id  {:keys [camera objects]}]
  (let [frame-chan  (a/chan 20)
        scene-chan  (a/chan 10)
        camera (c/make-camera! canvas-dom-id camera)
        camera-chan (c/add-camera+ camera canvas-dom-id)]
    (game-loop
     (merge {:renderer renderer
             :clock (t/Clock.)
             :scene (create-scene)}
            camera)
     [scene-chan frame-chan camera-chan])
    (frame-loop frame-chan)
    (a/go (a/>! scene-chan
                (.getObject (::c/controls camera))))
    (a/pipe (draw-scenes+ renderer objects) scene-chan)))


(defn draw [canvas-dom-id scene]
  (-> (create-renderer)
      (replace-canvas canvas-dom-id)
      (render-scene canvas-dom-id scene)))


;; (render-canvas renderer "canvas")
