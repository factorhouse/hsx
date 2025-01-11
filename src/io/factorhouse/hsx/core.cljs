(ns io.factorhouse.hsx.core
  (:require ["react" :as react]
            [goog.object :as obj]
            [io.factorhouse.hsx.props :as props]))

(declare create-element)

(defn- hsx-fn?
  [x]
  (or (fn? x)
      (instance? cljs.core/MultiFn x)))

(defn- create-element-vector
  [[elem-type & args :as hiccup]]
  (cond
    (= :<> elem-type)
    (apply react/createElement react/Fragment nil (map create-element args))

    (= :f> elem-type)
    (do
      (js/console.warn "Annotating components with hooks is a Reagent thing. Just use literal Hiccup instead.")
      (assert (hsx-fn? (first args))
              (str "To uphold the :f> syntax contact, the second element must be a ClojureScript function. Got: " (pr-str hiccup)))
      (create-element (vec args)))

    (= :> elem-type)
    (let [[f & args] args
          props    (props/hsx-props->react-props (first args))
          children (if props
                     (rest args)
                     args)
          props    (or props #js {})]
      (assert (or (fn? f) (object? f))
              (str "To uphold the :> syntax contract, the second element must be a React function component or class. Got: " (pr-str hiccup)))
      (when-let [meta-props (meta hiccup)]
        (obj/extend props (props/hsx-props->react-props meta-props)))
      (apply react/createElement f props (map create-element children)))

    (hsx-fn? elem-type)
    (let [f            (fn [] (create-element (apply elem-type args)))
          outer-props  (meta hiccup)
          display-name (or (:display-name outer-props) (:displayName outer-props) (pr-str elem-type))]
      (obj/set f "displayName" display-name)
      (react/createElement f (props/hsx-props->react-props outer-props)))

    (keyword? elem-type)
    (let [props    (props/hsx-props->react-props (first args))
          children (if props
                     (rest args)
                     args)
          props    (or props #js {})]
      (when-let [meta-props (meta hiccup)]
        (obj/extend props (props/hsx-props->react-props meta-props)))
      (apply react/createElement (name elem-type) props (map create-element children)))

    :else
    (throw (ex-info (str "Failed to create React element from provided Hiccup. Got: " (pr-str hiccup))
                    {:input hiccup}))))

(defn create-element
  "Like react/createElement, but takes in some Hiccup (HSX syntax) and returns a React element.

  Should be called at the root of your application, where you render your view:

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

   ```clojure
   (defn exported [props]
     [:div \"Hi, \" (:name props)])

  (def react-comp (reactify-component exported))

  (defn could-be-jsx []
    (react/createElement react-comp #js {:name \"world\"}))
   ```"
  [comp]
  (fn [props]
    (create-element [comp (js->clj props :keywordize-keys true)])))