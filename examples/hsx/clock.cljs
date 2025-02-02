(ns hsx.clock
  "Inspired by https://elm-lang.org/examples/clock"
  (:require ["react" :as react]))

(defn hand
  [width length turns]
  (let [t (* 2 js/Math.PI (- turns 0.25))
        x (+ 200 (* length (js/Math.cos t)))
        y (+ 200 (* length (js/Math.sin t)))]
    [:line {:x1 "200" :y1 "200"
            :x2 (str x)
            :y2 (str y)
            :stroke "black"
            :stroke-width (str width)
            :stroke-linecap "round"}]))

(defn clock
  []
  (let [[date set-date] (react/useState (js/Date.))]
    (react/useEffect
      (fn []
        (let [interval (js/setTimeout #(set-date (js/Date.)) 1000)]
          #(js/clearInterval interval))))

    [:svg {:viewBox "0 0 400 400" :width "400" :height "400" :xmlns "http://www.w3.org/2000/svg"}
     [:circle {:cx "200" :cy "200" :r "120" :fill "#CCCCCC"}]
     [hand 6 60 (/ (.getHours date) 12)]
     [hand 6 90 (/ (.getMinutes date) 60)]
     [hand 3 90 (/ (.getSeconds date) 60)]]))