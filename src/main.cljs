(ns main
  (:require [goog.dom :as gdom]
            [todomvc.core :as todo]
            [tiltontec.model.core :refer [mget]]
            [tiltontec.web-mx.api :refer [tag-dom-create]]))

(defn clear-root [root]
  (set! (.-innerHTML root) nil))

(defn load-app [root]
  (let [app-matrix (todo/matrix-build!)
        app-dom (tag-dom-create
                 (mget app-matrix :mx-dom))]
    (gdom/appendChild root app-dom)))

;; start is called by init and after code reloading finishes
(defn ^:dev/after-load start []
  (let [root (gdom/getElement "app")]
    (load-app root))
  (js/console.log "start"))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (js/console.log "init")
  (let [root (gdom/getElement "app")]
    (clear-root root)
    (load-app root)))

;; this is called before any code is reloaded
(defn ^:dev/before-load stop []
  (let [root (gdom/getElement "app")]
    (clear-root root))
  (js/console.log "stop"))
