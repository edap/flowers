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
                                            seventh-ring nil
                                            sixth-ring nil
                                            fifth-ring nil
                                            forth-ring nil
                                            third-ring nil
                                            second-ring nil
                                            first-ring])
        hex         (mg/apply-recursively (mg/reflect :w :out [slices slices]) 5 [1] 1)
        seed        (mg/sphere-lattice-seg 6 0.455 0.1355 0.38)]
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
  []
  (let [leafs  (mg/extrude :dir :s :len 0.8 :out [(leaf 15 0.8)])
        slices (mg/subdiv :slices 10 :out [leafs nil nil (bud) nil nil nil nil (middle)])
        side   (mg/apply-recursively (mg/reflect :e :out[slices slices]) 15 [1] 1)
        seed   (mg/sphere-lattice-seg 16 2.855 0.3 0.2)]
    [seed side]))

(defn save-mesh
  [seed tree]
  (-> seed
      (mg/seed-box)
      (mg/save-mesh tree)))
