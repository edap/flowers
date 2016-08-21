(ns flowers.core
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.aabb :as a]
   [thi.ng.morphogen.core :as mg]))

;; HELPERS

(defn punch
  "(punch :w 0.999) it's the same as
  {:op :sd-inset, :args {:dir :w, :inset 0.999}, :out [{} {} {} {} nil]}
  copied from http://media.thi.ng/2014/talks/we-are-the-incanters/index.html#/sec-54-5"
  [dir w & [out]]
  (mg/subdiv-inset :dir dir :inset w :out (or out {4 nil})))

;; SPRITZ FLOWER
(defn petal
  [scale offset punch-y]
  (let [punch-it (mg/skew :e :e :offset offset
                                :out [(punch :y punch-y)])
        tree     (mg/scale-edge :ef :y :scale scale
                                :out[punch-it])]
    tree))

(defn spritz
  []
  (let [first-ring   (mg/reflect :w :out [(petal 3.35 0.6 0.45)])
        second-ring  (mg/reflect :w :out [(petal 4.35 0.6 0.40)])
        third-ring   (mg/reflect :w :out [(petal 5.35 0.6 0.35)])
        forth-ring   (mg/reflect :w :out [(petal 6.35 0.6 0.30)])
        fifth-ring   (mg/reflect :w :out [(petal 7.35 0.6 0.25)])
        sixth-ring   (mg/reflect :w :out [(petal 8.35 0.6 0.15)])
        seventh-ring (mg/reflect :w :out [(petal 9.35 0.6 0.05)])
        ;; the rings could have been done programmatically but for
        ;; the sake of clarity I've preferred this more verbose way
        slices       (mg/subdiv :rows 13 :out[
                                            seventh-ring  nil
                                            sixth-ring  nil
                                            fifth-ring  nil
                                            forth-ring  nil
                                            third-ring nil
                                            second-ring nil
                                            first-ring])
        hex         (mg/apply-recursively (mg/reflect :w :out [slices slices]) 5 [1] 1)
        seed        (mg/sphere-lattice-seg 6 0.455 0.1355 0.38)]
    [seed hex]))

(defn save-mesh
  [seed tree]
  (-> seed
      (mg/seed-box)
      (mg/save-mesh tree)))
