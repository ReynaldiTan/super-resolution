(ns sr.projective
  (:require [incanter.core :as i])
  (:require [quil.core :as q])
  (:import processing.core.PImage)
  (:use sr.projective.one)
  (:use sr.projective.two)
  (:use clojure.tools.logging)
  (:use sr.logging)
  (:use sr.math)
  (:use sr.util))

(defmulti p
  "Return a function that is the projective transformation
  specified by parameters [a, b, c]."
  matricies?)

(defmethod p :matrix
  [a b c]
  (fn [x]
    (let [c (i/trans c)
          n (product-sum a x b)
          d (product-sum c x 1)]
      (i/div n d))))

(defmethod p :scalar ; should this include :matrix-and-scalar?
  [a b c]
  (fn [x]
    (let [n (product-sum a x b)
          d (product-sum c x 1)]
      (i/div n d))))

(defn projective-identity
  "The identity element of the projective group.
  
  dim should be 1 or 2 (the dimension of the images the transformation
  is defined on)."
  [dim]
  {:pre (#{1 2} dim)}
  (case dim
    1 (p 1 0 0)
    2 (p (i/identity-matrix 2) (i/matrix [0 0]) (i/matrix [0 0]))))


(defmulti make-transformation
  "Create the projective transformation function."
  (fn [dimension points]
    dimension))

(defmethod make-transformation 1 ;; make transformation for 1-D
  [_ points]
  {:pre [(seq? points)]}
  (let [points (map (partial map-vals first) points)
        u (U1D (map :x points))
        A (A1D (map #(vector (:x %) (:u %)) points))]
     (println)
     (println "**** u = " u ", A = " A)
     (println)
     (let [[a b c] (mult (i/solve A) u)]
       (p a b c))))

(defmethod make-transformation 2
  [_ points]
  (let [_ (spy points)
        ps' (map :u points)
        ps (map :x points)
        _ (spy ps')
        _ (spy ps)
        _ (prn "about to run homography")
        M (spy (homography ps' ps))
        _ (spy M)
        ]
    (homography-matrix-as-fn M)))

(defmethod make-transformation 3 ;; make-transformation for 2-D
  [_ points]
  (let [xs' (column (map (comp first :u) points))
        ys' (column (map (comp second :u) points))
        Ax (A2DX (map (juxt :x :u) points))
        Ay (A2DY (map (juxt :x :u) points))
        [a21 a22 b2 c1 c2] (mult (i/solve Ax) xs')
        bb (BY c1 c2 (map (juxt :x :u) points))
        X (i/matrix (map (comp (juxt first second (constantly 1)) :x) points))]
    (println
      "xs=" xs'
      "ys=" ys'
      "Ax=" Ax
      "Ay=" Ay
      "c1=" c1
      "c2=" c2
      "bb=" bb
      "X=" X)
    (projective-identity 2) 
    ))

(defn calculate-transformations
  [data]
  (let [dim (get-in data [:dimension])
        features (get-in data [:feature-match :features])
        f (fn [m [k v]]
            (assoc m k (spy (make-transformation dim v))))
        test (fn [m [k v]] (assoc m k (constantly k)))]
    (spy (reduce f {} features))))



