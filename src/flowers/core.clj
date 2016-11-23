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

(defn make-stripes
  "Returns a tree which subdivides form into `n` columns and only
  keeps those for whose index the given predicate returns a truthy
  value. If no predicate is given, `even?` is used by default.
  copied from https://github.com/thi-ng/morphogen/blob/master/src/examples.org"
  ([n] (make-stripes even? n))
  ([pred n]
   (mg/subdiv :cols n :out (mapv #(if (pred %) {}) (range n)))))

(defn make-net
  "divide per cols and rows, then, foreach square, perform a subdiv-inset
  ex (make-net 2 2 0.2)"
  ([nrows ncols inset]
  (let [net       (mg/subdiv :rows nrows :cols ncols)
        make-hole (constantly (mg/subdiv-inset
                               :dir :z :cols 3 :inset inset :out[0 1 2 3 nil]))
        tree      (mg/map-leaves make-hole net)
        ]
    tree)))

;; SPRITZ FLOWER
(defn petal
  [scale offset punch-y]
  (let [punch-it (mg/skew :e :e :offset offset
                                :out [(punch :y punch-y)])
        tree     (mg/scale-edge :ef :y :scale scale
                                :out[punch-it])]
    tree))

(defn spritz
  "it returns a seed and an operation"
  []
  (let [first-ring   (mg/reflect :w :out [(petal 3.35 0.6 0.45)])
        second-ring  (mg/reflect :w :out [(petal 4.35 0.6 0.40)])
        third-ring   (mg/reflect :w :out [(petal 5.35 0.6 0.35)])
        forth-ring   (mg/reflect :w :out [(petal 6.35 0.6 0.30)])
        fifth-ring   (mg/reflect :w :out [(petal 7.35 0.6 0.25)])
        sixth-ring   (mg/reflect :w :out [(petal 8.35 0.6 0.15)])
        seventh-ring (mg/reflect :w :out [(petal 9.35 0.6 0.05)])
        eigth-ring (mg/reflect :w :out [(petal 10.35 0.6 0.0010)])
        ;; the rings could have been done programmatically but for
        ;; the sake of clarity I've preferred this more verbose way
        slices       (mg/subdiv :rows 13 :out[
                                            eigth-ring  nil
                                            seventh-ring nil
                                            sixth-ring nil
                                            fifth-ring nil
                                            forth-ring nil
                                            third-ring nil
                                            second-ring nil
                                            first-ring])
        hex         (mg/apply-recursively (mg/reflect :w :out [slices slices]) 7 [1] 1)
        seed        (mg/sphere-lattice-seg 8 1.605 0.10155 0.68)]
    [seed hex]))

;; FAKE-TEASEL
(defn leaf
  [n-stripes width]
  (let [
        end    (mg/scale-edge :ab :x
                              :scale 0.02
                              :out [(make-stripes n-stripes)])
        middle (mg/reflect :s :out[{}
                                   (mg/scale-edge :bc :z
                                                  :len width
                                                  :out [(mg/extrude
                                                         :dir :s
                                                         :len 1.5
                                                         :out [end])])])
        attach (mg/split-displace
                :y :z
                :offset 0.4
                :out [middle {}])]
    attach))

(defn beak
  [len]
  (let [cave (mg/subdiv-inset :dir :y
                              :cols 3
                              :inset 0.02
                              :out[nil 1 2 3 nil])
        body (mg/subdiv :rows 3 :out [nil (mg/extrude :dir :s
                                                      :len len
                                                      :out[cave]) nil])]
    body))

(defn pistil
  [len]
  (mg/reflect :s :out [{} (beak len)]))

(defn bud
  []
  (let [cols     (mg/subdiv :slices 12 :out [nil (pistil 1.4)
                                             nil (pistil 1.1)
                                             nil (pistil 0.8)
                                             nil (pistil 0.5)
                                             nil (pistil 0.3)
                                             nil (pistil 0.1)])
        substain (mg/split-displace :y :z
                                    :offset 0.2
                                    :out[(mg/extrude :dir :f :out[cols]) {}])
        middle   (mg/extrude :len 0.12
                             :out[(mg/reflect :s :out[{} substain])])
        attach   (mg/replicate :s
                               :out[nil (mg/scale-edge :bc :z
                                                       :len 0.2
                                                       :out[middle])])]
    attach))

(defn middle
  []
  (let [net  (mg/scale-edge :ef :x :len 1.2 :out [(make-net 4 2 0.015)])
        body (mg/extrude :dir :n :len 0.4 :out [net])]
    body))

(defn fake-teasel
  "it returns a seed and an operation"
  []
  (let [leafs  (mg/extrude :dir :s :len 0.8 :out [(leaf 15 0.8)])
        slices (mg/subdiv :slices 10 :out [leafs nil nil (bud) nil nil nil nil (middle)])
        side   (mg/apply-recursively (mg/reflect :e :out[slices slices]) 15 [1] 1)
        seed   (mg/sphere-lattice-seg 16 2.855 0.3 0.2)]
    [seed side]))

;; CATHERINE-WHEEL
(defn scale-side
  [hex distorsion]
  (mg/scale-edge :ef :y
                 :scale distorsion
                 :out [hex]))

(defn displace-ring
  [& {:keys [offset scale] :or {offset 0.2 scale 1.0}}]
  (let [displace-it (mg/split-displace :y :z :offset offset :out [nil {}])
        scale-it    (mg/scale-edge :bc :z :scale scale :out [displace-it])]
    scale-it))

(defn catherine-wheel
  "it returns a seed and an operation, it accepts 3 parameters, offset, scale and y-distorsion"
  [& {:keys [offset scale y-distorsion]
      :or {offset 1.2 scale 1.35 y-distorsion 1.0}}]
  (let[
       n-slices 500
       n-rings 30
       nil-values     (vec (replicate n-slices nil))
       slices-indexes (reverse(take n-rings(iterate (partial * 0.8) (- n-slices 1))))
       slices-indexes (distinct(map int slices-indexes))
       scale-values   (take n-rings (iterate (partial * scale) 0.05))
       offset-values  (take n-rings (iterate (partial * offset) 0.05))
       rings          (map #(displace-ring :offset %1 :scale %2) offset-values scale-values)
       indx-and-rings (interleave slices-indexes rings)
       roses          (apply assoc nil-values indx-and-rings)
       slices         (mg/subdiv :slices n-slices :out roses)
       side           (mg/apply-recursively (mg/reflect :e :out[slices slices]) 23 [1] 1)
       side           (scale-side side y-distorsion)
       seed           (mg/sphere-lattice-seg 24 1.955 0.0955 0.6)]
    [seed side]))

;; FLW
(defn flw
  "it returns a seed and an operation"
  ([](flw 8 0.405 0.1215 0.2 0.4 0.4))
  ([n_petali](flw n_petali 8.35 0.405 0.1215 0.2 0.4 0.4))
  ([n_petali height-sphere](flw n_petali height-sphere 8.35 0.1215 0.2 0.4 0.4))
  ([n_petali height-sphere height-petals](flw n_petali height-sphere height-petals 0.1215 0.2 0.4 0.4))
  ([n_petali height-sphere height-petals inclinazione](flw n_petali height-sphere height-petals inclinazione 0.2 0.4 0.4))
  ([n_petali height-sphere height-petals inclinazione wall](flw n_petali height-sphere height-petals inclinazione wall 0.4 0.4))
  ([n_petali height-sphere height-petals inclinazione wall outset](flw n_petali height-sphere height-petals inclinazione wall outset 0.4))
  ([n_petali height-sphere height-petals inclinazione wall outset hole]
   (let [
         fifth-ring   (mg/reflect :w :out [(petal height-petals outset hole )])
         slices       (mg/subdiv :rows 13 :out[fifth-ring  nil])
         hex         (mg/apply-recursively (mg/reflect :w :out [slices slices]) (- n_petali 1) [1] 1)
         seed        (mg/sphere-lattice-seg n_petali height-sphere inclinazione wall)
         ]
     [seed hex])))

(defn save-mesh
  [seed tree]
  (-> seed
      (mg/seed-box)
      (mg/save-mesh tree)))
