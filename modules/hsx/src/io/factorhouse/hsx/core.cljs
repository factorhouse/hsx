(ns io.factorhouse.hsx.core
  (:require ["react" :as react]
            [goog.object :as obj]
            [io.factorhouse.hsx.props :as props]
            [io.factorhouse.hsx.tag :as tag]))

(declare create-element)

(defn- hsx-component?
  [x]
  (or (fn? x)
      (instance? cljs.core/MultiFn x)))

(defn- hsx-component->display-name
  [f]
  (try
    (or (.-displayName f)
        (.-name f)
        (pr-str f))
    (catch :default _
      (js/console.warn "Failed to construct a display name from HSX component, returning 'Unknown'")
      "Unknown")))

(defn- react-component?
  [x]
  ;; Naive predicate but good enough, does not check  x.prototype.isReactComponent for class components etc...
  ;; Which is fine as React compiler will throw a less specific exception in this circumstance
  (or (fn? x)
      (object? x)))

(defn- create-react-element
  [original-hsx elem props children]
  (try
    (apply react/createElement elem props children)
    (catch :default e
      (throw (ex-info "Failed to create React Element from provided HSX: exception calling react/createElement."
                      {:hsx        original-hsx
                       :elem       elem
                       :props      props
                       :children   children
                       :error-type :react-error}
                      e)))))

(defn- hsx-props->react-props
  [original-hsx props]
  (try (props/hsx-props->react-props props)
       (catch :default e
         (throw (ex-info "Failed to create React Element from provided HSX: Clj->JS props serialization error."
                         {:hsx        original-hsx
                          :props      props
                          :error-type :props-serialization-error}
                         e)))))

(defn- create-element-vector
  [[elem-type & args :as hsx]]
  (cond
    (= :<> elem-type)
    (create-react-element hsx react/Fragment nil (map create-element args))

    (= :f> elem-type)
    (do
      (js/console.warn "Annotating components for hooks (:f>) is a Reagent thing. Just call the component normally instead: "
                       (pr-str [(hsx-component->display-name (second hsx)) "..."]))
      (when-not (hsx-component? (first args))
        (throw (ex-info "Failed to create React Element from provided HSX: the second argument to :f> must be a ClojureScript function."
                        {:hsx        hsx
                         :elem       (second args)
                         :error-type :syntax-error})))
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
    (let [outer-props  (meta hsx)
          display-name (or (:display-name outer-props)
                           (:displayName outer-props)
                           (hsx-component->display-name elem-type))
          f            (fn []
                         (let [evaled-elem (try (apply elem-type args)
                                                (catch :default e
                                                  (ex-info (str "Failed to create React Element from provided HSX: unhandled exception when evaluating HSX component named '" display-name "'.")
                                                           {:hsx          hsx
                                                            :display-name display-name
                                                            :elem         elem-type
                                                            :args         args
                                                            :error-type   :hsx-component-error}
                                                           e)))]
                           (create-element evaled-elem)))]
      (obj/set f "displayName" display-name)
      (create-react-element hsx f (hsx-props->react-props hsx outer-props) nil))

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
    (throw (ex-info (str "Failed to create React element from provided HSX: cannot create element from type '" (type elem-type) "'.")
                    {:hsx        hsx
                     :elem       elem-type
                     :error-type :unknown-element-type}))))

(defn create-element
  "Like react/createElement, but takes in some HSX and returns a React element.

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