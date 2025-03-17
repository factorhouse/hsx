(ns io.factorhouse.hsx.core
  (:require-macros [io.factorhouse.hsx.core])
  (:require ["react" :as react]
            [clojure.string :as str]
            [goog.object :as obj]
            [io.factorhouse.hsx.props :as props]
            [io.factorhouse.hsx.tag :as tag]))

(def ^:private react-memo react/memo)
(defn- set-display-name [comp display-name] (obj/set comp "displayName" display-name))
(def ^:private obj-get obj/get)

(goog-define USE_MEMO true)

(declare create-element)

(goog-define ERROR-HANDLER "throw-ex-info")

(defmulti handle-error (fn [handler-id _message _ctx _error] handler-id))

(defmethod handle-error :throw-ex-info
  [_ message ctx error]
  (throw (ex-info message ctx error)))

(defn- handle-error*
  [message ctx error]
  (handle-error (keyword ERROR-HANDLER) message ctx error))

(defn- multi-method?
  [x]
  (instance? cljs.core/MultiFn x))

(defn- anon-hsx-component?
  [x]
  (or (fn? x) (multi-method? x)))

(defn- hsx-component->display-name
  [f]
  (try
    (let [display-name (or (.-displayName f)
                           (if (multi-method? f)
                             (some-> (.-name f) (obj/get "str"))
                             (.-name f)))]
      (if-not (str/blank? display-name)
        (let [display-name (-> display-name (str/replace "_" "-") (str/split "$"))]
          (if (= 1 (count display-name))
            (str "$hoc/" (first display-name))
            (str (str/join "." (butlast display-name)) "/" (last display-name))))
        "$hoc"))
    (catch :default _
      (js/console.warn "Failed to construct a display name from HSX component, returning nil."))))

(def ^:private react-special-components
  #{"react.profiler"
    "react.strict_mode"
    "react.suspense"})

(defn- react-component?
  [x]
  ;; Naive predicate but good enough, does not check  x.prototype.isReactComponent for class components etc...
  ;; Which is fine as React compiler will throw a less specific exception in this circumstance
  (or (fn? x)
      (object? x)
      ;; for react/Profiler etc
      (and (identical? (type x) js/Symbol)
           (contains? react-special-components (.-description x)))))

(defn- create-react-element
  [original-hsx elem props children]
  (try
    (apply react/createElement elem props children)
    (catch :default e
      (handle-error*
       "Failed to create React Element from provided HSX: exception calling react/createElement."
       {:hsx        original-hsx
        :elem       elem
        :props      props
        :children   children
        :error-type :react-error}
       e))))

(defn- hsx-props->react-props
  [original-hsx props]
  (try (props/hsx-props->react-props props)
       (catch :default e
         (handle-error*
          "Failed to create React Element from provided HSX: Clj->JS props serialization error."
          {:hsx        original-hsx
           :props      props
           :error-type :props-serialization-error}
          e))))

(defn- anon-hsx-comp-factory
  [elem-f]
  (fn anon-hsx-comp-proxy [props]
    (let [elem-args (obj/get props "args")
          comp*     (try (apply elem-f elem-args)
                         (catch :default e
                           (let [display-name (hsx-component->display-name elem-f)]
                             (handle-error* (str "Unhandled exception calling HSX component " display-name)
                                            {:props      elem-args
                                             :error-type :unhandled-exception
                                             :elem       elem-f}
                                            e))))]
      (create-element comp*))))

(defn- are-props-equal?
  [prev-props next-props]
  (= (obj/get prev-props "args")
     (obj/get next-props "args")))

;; The way that React function components work (especially with hooks and react/memo) is based on referential equality:
;; objects are considered equal based on their memory location and not their value.
;;
;; In order for us to provide a 'Reagent facade' - that is, something to convert from a single-arg React function component with JS props
;; to a (potentially) multi-arg Clojure function accepting any sort of type as its arguments, we need to return the
;; same facade component (the return value of `anon-hsx-comp-factory`) every time.
;;
;; To stop potentially unbounded memory growth, we use a JS WeakMap as our cache: where the keys are the Reagent function component objects.
;; When the Reagent functions get GC'd, the key is also removed from the weak map cache.
;;
;; Further reading:
;; - https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakMap/WeakMap
;; - https://react.dev/reference/react/memo
;; - https://blog.bitsrc.io/understanding-referential-equality-in-react-a8fb3769be0
(defonce ^:private component-cache
  (volatile! (js/WeakMap.)))

(defn- anon-hsx-component
  [elem-f memo?]
  (let [weak-map ^js @component-cache]
    (if-let [proxy-comp (.get weak-map elem-f)]
      proxy-comp
      (let [proxy-comp' (anon-hsx-comp-factory elem-f)
            proxy-comp  (cond-> proxy-comp'
                          memo? (react/memo are-props-equal?))]
        (set-display-name proxy-comp' (hsx-component->display-name elem-f))
        (.set weak-map elem-f proxy-comp)
        proxy-comp))))

(defn memo-clear!
  "Resets the component cache. Useful to call in dev after hot reloading."
  []
  (vreset! component-cache (js/WeakMap.)))

(defrecord Component [])

(defn- hsx-component? [x]
  (instance? Component x))

(defn- create-element-vector
  [[elem-type & args :as hsx]]
  (cond
    (= :<> elem-type)
    (create-react-element hsx react/Fragment nil (map create-element args))

    (= :f> elem-type)
    (do
      (js/console.warn "Annotating components for hooks (:f>) is a Reagent thing. Just call the component normally: "
                       (pr-str [(hsx-component->display-name (second hsx)) "..."]))
      (when-not (anon-hsx-component? (first args))
        (handle-error*
         "Failed to create React Element from provided HSX: the second argument to :f> must be a ClojureScript function."
         {:hsx        hsx
          :elem       (second args)
          :error-type :syntax-error}
         nil))
      (create-element (with-meta (vec args) (meta hsx))))

    (= :> elem-type)
    (let [[f & args] args
          props    (hsx-props->react-props hsx (first args))
          children (if props
                     (rest args)
                     args)
          props    (or props #js {})]
      (when-not (react-component? f)
        (throw (ex-info "Failed to create React Element from provided HSX: the second argument to :> must be a valid React component (a function or class that extends React.Component)."
                        {:hsx        hsx
                         :elem       (second args)
                         :error-type :syntax-error
                         :docs       "https://react.dev/reference/react/createElement#parameters"})))
      (when-let [meta-props (meta hsx)]
        (obj/extend props (hsx-props->react-props hsx meta-props)))
      (create-react-element hsx f props (map create-element children)))

    (hsx-component? elem-type)
    (let [outer-props   (merge {:memo? USE_MEMO} (meta hsx))
          returned-comp (if (:memo? outer-props) (:proxy-memo elem-type) (:proxy elem-type))
          props         (or (hsx-props->react-props hsx outer-props) #js {})]
      (obj/extend props #js {"args" args})
      (create-react-element hsx returned-comp props nil))

    (anon-hsx-component? elem-type)
    (let [outer-props   (merge {:memo? USE_MEMO} (meta hsx))
          returned-comp (anon-hsx-component elem-type (:memo? outer-props))
          props         (or (hsx-props->react-props hsx outer-props)
                            #js {})]
      (when ^boolean js/goog.DEBUG
        (when (and (multi-method? elem-type) (not (:key outer-props)))
          (js/console.warn "HSX: multimethod component" (hsx-component->display-name elem-type) "should be created with ^:key metadata.")))

      (when ^boolean js/goog.DEBUG
        (let [display-name (hsx-component->display-name elem-type)]
          (when (and (str/starts-with? display-name "$hoc")
                     (or (not (:hoc outer-props))
                         (not (:hoc (meta elem-type)))))
            (js/console.warn "Higher-order components (HOC) are discouraged when using HSX. See: https://legacy.reactjs.org/docs/higher-order-components.html#dont-use-hocs-inside-the-render-method. If you know what you are doing, and want to ignore this warning, pass a ^:hoc key to your HOC. Hiccup: ^:hoc"
                             (pr-str (into [(symbol display-name)] args))
                             ". If your anonymous function is not a HOC (but a closure captured in a top-level def) then you can wrap your component with the `hsx.core/component` macro: `(hsx/component \"MyDisplayName\" (fn [] ...))`."))))

      (obj/extend props #js {"args" args})
      (create-react-element hsx returned-comp props nil))

    (keyword? elem-type)
    (let [{:keys [tag id className]} (tag/cached-parse elem-type)
          props    (hsx-props->react-props hsx (first args))
          children (if props
                     (rest args)
                     args)
          props    (or props #js {})]
      (when-let [meta-props (meta hsx)]
        (obj/extend props (hsx-props->react-props hsx meta-props)))
      (when id
        (obj/set props "id" id))
      (when className
        (obj/set props "className" (str (obj/get props "className") " " className)))
      (create-react-element hsx tag props (map create-element children)))

    :else
    (handle-error*
     (str "Failed to create React element from provided HSX: cannot create element from type '" (type elem-type) "'.")
     {:hsx        hsx
      :elem       elem-type
      :error-type :unknown-element-type}
     nil)))

(defn create-element
  "Like react/createElement, but takes in some HSX hiccup and returns a React element.

  Generally called at the root of your application, where you render your UI:

  ```clojure
  (defonce root
    (createRoot (.getElementById js/document \"app\")))

  (defn init []
    (.render root (hsx/create-element [:div \"Hello world!\"])))
  ```"
  [this]
  (cond
    (vector? this)
    (create-element-vector this)

    (seq? this)
    (into-array (map create-element this))

    :else
    this))

(defn reactify-component
  "Returns a React Function component that can be called outside a HSX context.

   This function is similar to reagent.core/reactify-component.

   Requires that the HSX component accept everything in a single props map, including its children.

   Second argument (optional): the props deserialization function. By default, is a shallow js->cljs.

   ```clojure
   (defn exported [props]
     [:div \"Hi, \" (:name props)])

  (def react-comp (reactify-component exported))

  (defn could-be-jsx []
    (react/createElement react-comp #js {:name \"world\"}))
   ```"
  ([comp]
   (reactify-component comp props/shallow-js->cljs))
  ([comp props-deserialization-fn]
   (fn reactify-component-proxy [props]
     (create-element [comp (props-deserialization-fn props)]))))
