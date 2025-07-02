(ns io.factorhouse.hsx.props
  "Adapted from https://github.com/reagent-project/reagent/blob/master/src/reagent/impl/template.cljs"
  (:require [clojure.string :as str]
            [goog.object :as obj]))

(def dont-camel-case
  #{"aria" "data"})

(defn capitalize
  [s]
  (if (< (count s) 2)
    (str/upper-case s)
    (str (str/upper-case (subs s 0 1)) (subs s 1))))

(defn dash-to-prop-name
  [dashed]
  (if (string? dashed)
    dashed
    (let [name-str (name dashed)
          [start & parts] (str/split name-str #"-")]
      (if (dont-camel-case start)
        name-str
        (apply str start (map capitalize parts))))))

(defn ^boolean named?
  [x]
  (or (keyword? x)
      (symbol? x)))

(def prop-name-cache
  #js {:class   "className"
       :for     "htmlFor"
       :charset "charSet"})

(defn cache-get
  [^js o k]
  (let [v (goog.object/get o k)]
    (when-not (undefined? v) v)))

(defn cached-prop-name
  [k]
  (if (named? k)
    (if-some [k' (cache-get prop-name-cache (name k))]
      k'
      (let [v (dash-to-prop-name k)]
        (obj/set prop-name-cache (name k) v)
        v))
    k))

(declare convert-prop-value)

(defn convert-class
  [klass]
  (let [classes (if (string? klass) [klass] klass)]
    (str/join " " classes)))

(defn kv-conv
  [o k v]
  (let [prop-name (cached-prop-name k)
        prop-value (if (= prop-name "className")
                     (convert-class v)
                     (convert-prop-value v))]
    (doto o
      (obj/set prop-name prop-value))))

(defn convert-prop-value
  [x]
  (cond
    (object? x)
    x

    (named? x)
    (name x)

    (map? x)
    (reduce-kv kv-conv #js{} x)

    (coll? x)
    (clj->js x)

    (ifn? x)
    (fn [& args]
      (apply x args))

    :else
    (clj->js x)))

(defn hsx-props->react-props
  [props]
  (cond
    (map? props)
    (reduce-kv kv-conv #js{} props)

    (object? props)
    props

    :else
    nil))

(defn shallow-js->cljs
  [x]
  (persistent!
   (reduce (fn [r k] (assoc! r (keyword k) (obj/get x k)))
           (transient {}) (js-keys x))))
