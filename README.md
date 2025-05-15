<img src="assets/hsx.svg" alt="HSX" width="240"/>

[![test](https://github.com/factorhouse/hsx/actions/workflows/test.yml/badge.svg?branch=main)](https://github.com/factorhouse/hsx/actions/workflows/test.yml)
[![Clojars Project](https://img.shields.io/clojars/v/io.factorhouse/hsx.svg)](https://clojars.org/io.factorhouse/hsx)

**HSX** is a ClojureScript library for writing React components using [Hiccup syntax](https://github.com/weavejester/hiccup). We believe Hiccup is the most idiomatic (and joyful) way to express HTML in Clojure.

Think of HSX as a lightweight syntactic layer over React, much like [JSX](https://react.dev/learn/writing-markup-with-jsx) in the JavaScript world.

## Why HSX?

HSX is designed to offer a seamless transition from Reagent to plain, idiomatic React Function components. It’s compatible with [Reagent-style Hiccup](https://github.com/reagent-project/reagent/blob/master/doc/UsingHiccupToDescribeHTML.md), making it trivial to migrate your existing codebase to HSX.

Unlike Reagent, HSX does not:

* Render components as classes (under the hood). HSX compiles to plain React Function components.
* Include its own state abstractions like Ratoms and reactions. Use React’s built-in state management hooks like [useState](https://react.dev/reference/react/useState).

If you want to read more about the engineering challenge of moving a 120k LOC Reagent codebase to React 19 read [this blog post](https://factorhouse.io/articles/evolving-beyond-reagent/).

## Features

* **Supports React 19:** hooks, effects, concurrent rendering, suspense, transitions, etc 
* **Hiccup Syntax**: Write React components with concise, readable Hiccup expressions.
* **Minimal Overhead**: HSX is just a thin layer on top of React. No unnecessary abstractions or runtime complexities. No external dependencies.
* **Migration-Friendly**: Drop-in compatibility with Reagent-style Hiccup makes it simple to upgrade existing codebases.

## Motivation

[Reagent](https://github.com/reagent-project/reagent) was ahead of its time, giving ClojureScript developers advanced tools like reactive atoms and declarative UI rendering. However, React has since evolved, and modern React features such as hooks and concurrent rendering are fundamentally incompatible with Reagent’s internals.

### Challenges with Reagent

* **React 19 Compatibility**: Reagent's rendering model [does not play nice](https://github.com/reagent-project/reagent/issues/597#issuecomment-1908054952) with current React versions.
* **Technical Debt**: Continuing to depend on Reagent introduces maintenance challenges. Reagent depends on a version of React that is over three years old. Most of the React ecosystem is starting require React 18 at a minimum.

## Usage

Using HSX is straightforward. The entire library is only about 300 lines of ClojureScript (with comments). HSX is designed to be as close to plain React as possible while retaining the expressive power of Hiccup and Clojure data structures.

HSX exposes two primary functions:

* `io.factorhouse.hsx.core/create-element` - like `react/createElement` but for HSX components
* `io.factorhouse.hsx.core/reactify-component` - like `reagent.core/reactify-component`

### Example

```clojure
(ns com.corp.my-hsx-ui
  (:require [io.factorhouse.hsx.core :as hsx]
            ["react-dom/client" :refer [createRoot]]))

;; This is a HSX component
(defn test-ui [props text]
  [:div props
   "Hello " text "!"])

(defonce root
  (createRoot (.getElementById js/document "app")))

(defn init []
  (.render root 
           (hsx/create-element 
             [test-ui {:on-click #(js/alert "Clicked!")} 
              "prospective HSX user"])))
```

See the [examples](https://github.com/factorhouse/hsx/tree/main/examples/hsx) directory for more examples.

## Migrating from Reagent

If you have an existing Reagent codebase, the following `reagent.core` functions map to:

| Reagent | HSX |
|----------|-------------|
| `reagent.core/as-element` | `io.factorhouse.hsx.core/create-element` |
| `reagent.core/reactify-component` | `io.factorhouse.hsx.core/reactify-component` |
| `reagent.core/create-element` | `react/createElement` |

## FAQs

### What about performance?

When migrating from Reagent you will objectively find performance wins for your application by:

- **Embracing concurrent rendering** - allowing React to [interrupt, schedule, and batch updates more intelligently](https://vercel.com/blog/how-react-18-improves-application-performance).
- **Eliminating class-based components**, which Reagent relied on under the hood, removing unnecessary rendering layers (via `:f>`) and improved interop with React libraries.
- **Fixing long-standing Reagent interop quirks** — such as the well-documented [controlled input hacks](https://github.com/reagent-project/reagent/issues/619).

When profiling our real-world, enterprise grade product ([Kpow](https://factorhouse.io/kpow)) we saw 4x fewer commits without the overall render duration blowing out after switching to HSX. More details [here]().

### What about Ratoms (local state)?

HSX components are just React function components under the hood with a bit of syntactic sugar. 

There are no state abstractions found in this library. We suggest you migrate any Reagent components with local state to use `react/useState`. The `useState` hook is the most idiomatic way to deal with local state in React.

```clojure
;; (:require ["react" :as react])

(defn reagent-component-with-local-state []
  (let [state (reagent.core/atom 1)]
    (fn []
      [:div {:on-click #(swap! state inc)} 
       "The value of state is " @state])))

(defn hsx-component-with-local-state []
  (let [[state set-state] (react/useState 1)]
    [:div {:on-click #(set-state inc)} 
     "The value of state is " state]))
```

### What about re-frame?

We have a companion library named [RFX](https://github.com/factorhouse/rfx) which is a drop-in replacement for re-frame without the dependency on Reagent.

See the [RFX repo](https://github.com/factorhouse/rfx) for more details.

### What about global application state?

If RFX is overkill for your application (or you have bespoke requirements), you can use standard React solutions for global application state management like:

* [useSyncExternalStore](https://react.dev/reference/react/useSyncExternalStore) hook to subscribe to an external store (such as a plain Clojure atom or even a [Datascript database](https://github.com/tonsky/datascript)).
* Solutions found in the JS ecosystem like [zustand](https://github.com/pmndrs/zustand).
* Using React [Reducer and Context](https://react.dev/learn/scaling-up-with-reducer-and-context) APIs.

### What about hot-reloading?

Using [shadow-cljs](https://github.com/thheller/shadow-cljs) add a reload function like:

```clojure
(defn ^:dev/after-load reload []
  (hsx/memo-clear!))
```

This will clear the component cache after a code change.

### How are props handled?

Exactly the same as Reagent:

```clojure
[:div {:on-click #(js/alert "Clicked!")}]
```

Would translate to:

```clojure
[:div #js {"onClick" #(js/alert "Clicked!")}]
```

We use the same props serialization logic as Reagent to make migrating to HSX as pain-free as possible.

### What about component metadata (keys, etc)

The same as Reagent - use Clojure metadata. Say you want to pass a React key to a component:

```clojure
(defn component-with-seq []
  [:ol {:className "bg-slate-500"}
   (for [item items]
     ^{:key (str "item-" (:id item))}
     [item-component item])])
```

### What about `id` and `className` short-hands?

The same as Reagent + Hiccup:

```clojure
[:div#foo ...] ;; => [:div {:id "foo"}]
[:div.foo.bar ...] ;; => [:div {:className "foo bar"}]
```

### What about Reagent class-based components?

Class based components (the ones with lifecycle methods) have been out of style for almost a decade with React.

If you wish to adopt HSX you will need to migrate Reagent class components to function components. Generally this means rewriting the component to use hooks. 

However, [error boundaries](https://react.dev/reference/react/Component#catching-rendering-errors-with-an-error-boundary) are the one place in the React ecosystem where class components may be required. We suggest using a wrapping library like [react-error-boundary](https://github.com/bvaughn/react-error-boundary) instead.

If you still require class-based components, you can always extend `js/React.Component.prototype` yourself. See [this gist](https://gist.github.com/pesterhazy/2a25c82db0519a28e415b40481f84554) for an example.

### Are components memoized (like Reagent's implicit `componentDidUpdate` logic)?

By default, yes, HSX components are wrapped in a [react/memo](https://react.dev/reference/react/memo) call with an appropriate `arePropsEqual?` predicate for ClojureScript data structures. 

**Note**: unlike Reagent, memoization is a performance optimization. Please refer to the [official React documentation](https://react.dev/reference/react/memo) for more information.

If you'd like to disable memoization by default globally, you can:

```clojure
{...
 :builds
 {:app
  {:target :browser
   :modules {:app {:entries [your.app]}}

   :closure-defines {io.factorhouse.hsx.core/USE_MEMO false}
   
   }}}
```

If you'd like to disable/enable memoization per-component, you can supply a `:memo?` key as metadata to the component vector:

```clojure
[:div 
 ^{:memo? false} 
 [my-hsx-comp arg1 arg2]]
```

If you want to use a custom `are-props-equal?` predicate for memoization, you can also use component metadata:

```clojure
;; This custom predicate treats the previous and next state as equal if the value of `:foo` has not changed.
(defn custom-are-props-equal-pred
  [[prev-arg1 _prev-arg2] [next-arg1 _next-arg2]]
  (= (:foo prev-arg1) (:foo next-arg1)))

[:div
 ^{:memo? true :memo/predicate custom-are-props-equal-pred}
 [my-hsx-comp arg1 arg2]]
```

### What about Fragments?

The same as Reagent. Denoted by `:<>`

```clojure
(defn list-of-things [] 
  [:<> 
   [:div "First thing"]
   [:div "Second thing"]])
```

### How do I call JavaScript components?

The same as Reagent. Denoted by `:>`

```clojure
;; (:require ["react-select" :as Select])

(defn dropdown-example [options]
  [:> Select {:on-change #(js/alert "Data changed")
              :options options}])
```

### Is there a way to bypass JavaScript component props serialization?

Yes, pass a JS Object instead:

```clojure
;; (:require ["react-select" :as Select])

(defn dropdown-example [options]
  [:> Select #js {"onChange" #(js/alert "Data changed") 
                  "options" options}])
```

### How do I use a HSX component from within a JavaScript component?

Use `hsx/reactify-component`. If we use [react-error-boundary](https://github.com/bvaughn/react-error-boundary) as an example:

```clojure
;; (:require ["react-error-boundary" :refer [ErrorBoundary]] 
;;           [io.factorhouse.hsx.core :as hsx])

(defn fallback-renderer 
  [{:keys [error]}]
  [:div (str "Something went wrong: " error)])

(defn with-error-boundary 
  [comp]
  [:> ErrorBoundary {:fallbackRender (hsx/reactify-component fallback-renderer)}
   (hsx/create-element comp)])

(defn my-ui []
  [with-error-boundary 
   [:div "This is my application..."]])
```

## Copyright and License

Copyright © 2025 Factor House Pty Ltd.

Distributed under the Apache-2.0 License, the same as Apache Kafka.
