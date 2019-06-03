(ns user
  (:require [clojure.spec.alpha :as s]
            [nrepl.server :as nrepl]
            [shadow.cljs.devtools.server :as server]
            [shadow.cljs.devtools.api :as shadow]
            [snow.env :as env]
            [dynadoc.core :as doc]
            ))

(s/check-asserts true)

#_(do (require '[expound.alpha :as expound])
      (set! s/*explain-out* expound/printer))


;; (defn cljs-repl []
;;   (cemerick.piggieback/cljs-repl :app))

;; (defn restart-systems! []
;;   (do (repl/stop!)
;;       (repl/start! system-config)))

#_(restart-systems!)

#_(cljs-repl)

;; (defn compile-cljs []
;;   (server/start!)
;;   (shadow/dev :app))

#_(compile-cljs)

#_(shadow/release :app)



(defn nrepl-handler []
  (require 'cider.nrepl)
  (ns-resolve 'cider.nrepl 'cider-nrepl-handler))


(defn start-nrepl []
  (nrepl/start-server :port (or (:repl-port (env/profile))
                               9001)
                      :handler (nrepl-handler)))

(defn -main [& args]
  (println "Starting vin repl...")
  ;; (repl/start! system-config)
  (start-nrepl)
  (println "nrepl started")
  (doc/start {:port 50000})
  (println "docs at port 50000")
  (server/start!)
  (shadow/dev :app))


#_(shadow/watch :test)

