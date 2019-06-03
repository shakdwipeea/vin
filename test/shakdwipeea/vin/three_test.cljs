(ns shakdwipeea.vin.three-test
  (:require [cljs.test :refer [deftest is async]]
            [shakdwipeea.vin.three :as t]
            [clojure.core.async :as a :refer [go >! <!]]))


(deftest to-channel+-with-channel
  (let [a (a/chan 1)]
    (async done
           (go (>! a 12))
           (go (is (= 13 (<! (t/to-channel+ a inc))))
               (done)))))

(deftest to-channel+-with-value
  (async done
         (let [ch (t/to-channel+ 12 inc)]
           (go (is (= 13 (<! ch)))
               (done)))))


(deftest async-mix-test
  (let [o (a/chan 1)
        mix (a/mix o)
        i (a/chan 1)]
    (async done
           (a/admix mix i)
           (go (>! i 12)
               (a/close! i)
               (is (= 12  (<! o)))
               (done)))))

(deftest next-frame
  (let [update-chan (a/chan 1)
        initial-game-state {:scene (t/create-scene)}
        frame-chan (t/get-next-frame+ initial-game-state
                                      (list update-chan))]
    (async done
           (go
             (>! update-chan {:type :frame})
             (is (= true (:render? (<! frame-chan))))
             (done)))))
