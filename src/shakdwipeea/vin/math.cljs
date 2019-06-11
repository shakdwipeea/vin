(ns shakdwipeea.vin.math
  #:ghostwheel.core{:check     true
                    :num-tests 10}
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :as g
             :refer [>defn >defn- >fdef => | <- ?]]))



(s/def ::vector (s/coll-of int? :count 3))

(>defn add-vector3
       "add vectors"
       [v1 v2]
       [::vector ::vector => ::vector]
       (map + v1 v2))

(defn move-x [op val [x y z]] [(op x val) y z])
(defn move-y [op val [x y z]] [x (op y val) z])
(defn move-z [op val [x y z]] [x y (op z val)])

(g/check)


