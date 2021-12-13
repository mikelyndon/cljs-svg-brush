(ns cljs-svg-brush.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]
   [oops.core :refer [oget]]
   [clojure.string :as string]))

;; -------------------------
;; Views

(def dots (r/atom []))
(def allowSketching (r/atom false))
(def shapes (r/atom []))

(defn getPointerPosition [canvas evt]
  (let [rect (-> canvas .getBoundingClientRect)]
    (list (* (/ (- (oget evt "clientX") (.-left rect)) (- (.-right rect) (.-left rect))) (oget canvas "clientWidth"))
          (* (/ (- (oget evt "clientY") (.-top rect)) (- (.-bottom rect) (.-top rect))) (oget canvas "clientHeight")))))

(defn handleMouseDown []
  (reset! allowSketching true))

(defn handleMouseMove [e]
  (when @allowSketching
    (swap! dots conj (getPointerPosition (.getElementById js/document "svg-canvas") e))))

(defn handleMouseUp []
  (reset! allowSketching false)
  (swap! shapes conj @dots)
  (reset! dots []))

(comment
  (print @shapes))

(defn gen-key [prefix]
  (gensym (str prefix "-")))

(defn SvgDots []
  [:g
   (doall
    (for [dot @dots]
      (let [idx (gen-key "dot")]
        [:circle {:key idx :id idx :cx (nth dot 0) :cy (nth dot 1) :r 2 :fill "#aaa"}])))])

(defn reduceToPath [points]
  (if (> (count points) 0)
    (reduce
     (fn a [prev point]
       (string/join " " (list prev "L" (nth point 0) (nth point 1))))
     (str "M " (nth (nth points 0) 0) " " (nth (nth points 0) 1))
     (subvec points 1))
    ""))

(defn SvgShape [{:keys [id points colour]}]
  [:g
   [:path {:id id :d (reduceToPath points) :fill "none" :stroke colour :strokeWidth 3}]])

(defn SvgShapes []
  [:g
   (doall
    (for [shape @shapes]
      (let [idx (gen-key "stroke")]
        [SvgShape {:key idx :id idx :points shape :colour "red"}])))])

(defn svg-canvas []
  (let [app (.getElementById js/document "app")
        width (.-clientWidth app)
        height (.-clientHeight app)]
    [:svg {:id "svg-canvas"
           :width width
           :height height
           :viewBox [0 0 width height]
           :onMouseDown handleMouseDown
           :onMouseMove handleMouseMove
           :onMouseUp handleMouseUp}
     [:rect {:id "rectangle" :width width :height height :fill "#eee"}]
     [SvgShapes]
     [SvgShape {:key "current-stroke" :id "current-stroke" :points @dots :colour "red"}]
     [SvgDots]]))

(defn home-page []
  [:div
   [svg-canvas]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))
