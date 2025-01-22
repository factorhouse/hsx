# HSX

[![test](https://github.com/factorhouse/hsx/actions/workflows/test.yml/badge.svg?branch=main)](https://github.com/factorhouse/hsx/actions/workflows/test.yml)

**HSX** is a ClojureScript library for writing React components using [Hiccup syntax](https://github.com/weavejester/hiccup). We believe Hiccup is the most idiomatic way to express HTML in Clojure.

Think of HSX as a lightweight syntactic layer over React, much like [JSX](https://react.dev/learn/writing-markup-with-jsx) in the JavaScript world.

## Why HSX?

HSX is designed to offer a seamless transition from Reagent-style development to plain React Function components. It’s compatible with [Reagent-style Hiccup](https://github.com/reagent-project/reagent/blob/master/doc/UsingHiccupToDescribeHTML.md), making it easy to migrate your codebase to HSX.

Unlike Reagent, HSX does not:

* Render components as classes. HSX elements are plain React Function components.
* Include its own state abstractions like RAtom. Use React’s built-in state management hooks like [useState](https://react.dev/reference/react/useState).

If you want to read more about the engineering challenge of moving a 100k+ LOC Reagent codebase to React 19 read [this blog post]().

## Features

* **Supports React 19:** hooks, effects, concurrent rendering, suspense, transitions, etc 
* **Hiccup Syntax**: Write React components with concise, readable Hiccup expressions.
* **Minimal Overhead**: HSX is just a thin layer on top of React. No unnecessary abstractions or runtime complexities. No external dependencies.
* **Migration-Friendly**: Drop-in compatibility with Reagent-style Hiccup makes it simple to upgrade existing codebases.

## Motivation

[Reagent](https://github.com/reagent-project/reagent) was ahead of its time, giving ClojureScript developers advanced tools like reactive atoms and declarative UI rendering. However, React has since evolved, and modern React features such as hooks and concurrent rendering are fundamentally incompatible with Reagent’s internals.

### Challenges with Reagent

* **React 19 Compatibility**: Reagent's rendering model [does not work](https://github.com/reagent-project/reagent/issues/597#issuecomment-1908054952) with current React versions
* **Technical Debt**: Continuing to depend on Reagent introduces maintenance challenges. Reagent depends on a version of React that is 4+ years old. Most of the React ecosystem is starting require React 18 at a minimum.

## Usage

Using HSX is straightforward. The entire library is only about 150 lines of ClojureScript (with comments), and there are no macros or complicated rendering mechanisms involved. It’s designed to be as close to plain React as possible while retaining the expressive power of Hiccup.

HSX exposes two primary functions:

* `io.factorhouse.hsx.core/create-element` - like `react/createElement`
* `io.factorhouse.hsx.core/reactify-component` - like `reagent.core/reactify-component`

### Example

```clojure
(ns com.corp.my-hsx-ui
  (:require [io.factorhouse.hsx.core :as hsx]
            ["react-dom" :refer [createRoot]]))

(defn test-ui []
  [:div {:on-click #(js/alert "Clicked!")}
   "Hello world"])

(defonce root
  (createRoot (.getElementById js/document "app")))

(defn init []
  (.render root (hsx/create-element [test-ui])))
```

## FAQs

### What about performance? 

Migrating from Reagent you will objectively find massive performance wins for your application because:

* You ditch the years of baggage Reagent has carried (RAtoms, reactions, etc)
* You get the performance improvements found in [recent React versions](https://vercel.com/blog/how-react-18-improves-application-performance)

### What about RAtoms (local state)?

HSX components are just React Function components under the hood with a bit of syntactic sugar. 

There are no state abstractions found in this library. We suggest you migrate any Reagent components with local state to use `react/useState`. The state hook is the most idiomatic way to deal with local state in React.

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

We have a companion library named [un-frame](https://github.com/factorhouse/un-frame) which is a drop-in replacement for re-frame without the dependency on Reagent. 

See the un-frame repo for more details.

### What about global application state?

If re-frame is overkill for your application (or you have bespoke requirements), you can use standard React solutions to global application state management like:

* [useSyncExternalStore](https://react.dev/reference/react/useSyncExternalStore) hook to subscribe to an external store (such as a plain Clojure atom or even a [Datascript database](https://github.com/tonsky/datascript))
* Solutions found in the JS ecosystem like [zustand](https://github.com/pmndrs/zustand)
* Using React [Reducer and Context](https://react.dev/learn/scaling-up-with-reducer-and-context) APIs


### How are props handled?

Exactly the same as Reagent:

```clojure 
[:div {:on-click #(js/alert "Clicked!")}]
```

Would translate to:

```clojure 
[:div #js {"onClick" #(js/alert "Clicked!")}]
```

We use the same props serialisation logic as Reagent to make migrating to HSX as pain-free as possible.

### What about component metadata (keys, display name etc)

The same as Reagent - use Clojure metadata. Say you want to pass a React key to a component:

```clojure 
(defn component-with-seq []
  [:ol
   (for [item items]
     ^{:key (str "item-" (:id item))}
     [item-component item])])
```

### What about Reagent class-based components?

Class based components (the ones with lifecycle methods) have been out of style for almost a decade with React. 

If you wish to adopt HSX you will need to migrate Reagent class components to Function components.

Generally this means rewriting the component to use hooks. 

However, the one place in the React ecosystem where Class components may be required are [error boundaries](https://react.dev/reference/react/Component#catching-rendering-errors-with-an-error-boundary). 

If the only place you are using Reagent class components is for error boundaries (like us) then we suggest using a library like [react-error-boundary](https://github.com/bvaughn/react-error-boundary) instead.

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

### How do I use a HSX component from within a JavaScript component?

If we use [react-error-boundary](https://github.com/bvaughn/react-error-boundary) as an example:

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

### How are errors handled?


HSX has a customisable error handler. By default, an `ex-info` is thrown when an error is encountered. This `ex-info` object will contain:

* The message - a human-readable message describing the error
* The ex-data - context about the error: the originating hiccup, the failing element, the type of error and so forth
* The originating error (if-any)

You can classify HSX errors as syntax errors (eg malformed Hiccup) thrown when HSX attempts to compile your code to React elements. HSX's error handling will not pick up errors thrown during the React render lifecycle, eg errors caused by invalid business logic within your app.

Error handling is extensible!

You could have both a production and dev error handler:

* Your production error handler might tap into some sort of error tracking backend like Sentry.
* Your dev error handler might log to the console and return some Hiccup describing the error that is meaningful to developers.

You can customise the error handler using `closure-defines`. Within your `shadow-cljs.edn` file:

```clojure
{...
 :builds
 {:app
  {:target :browser
   ...
   :modules {:app {:entries [your.app]}}
   ;; to enable in development only
   :dev {:closure-defines {io.factorhouse.hsx/ERROR-HANDLER "dev"}}

   :release {:closure-defines {io.factorhouse/ERROR-HANDLER "prod"}}
   }}
```

You can then extend the `io.factorhouse.hsx.core/error-handler` multimethod for each of your error handlers:

```clojure
(defmethod hsx/error-handler :dev
  [_ message ctx e]
  (js/console.warn "Error compiling HSX" message e)
  ;; Return some HSX
  [:<>
    [:h1 "Error compiling HSX"]
    [:p message]
    [:pre (pr-str ctx)]
    [:p "Please check the output of your browser's console for more details."]]
```

```clojure
(defmethod hsx/error-handler :prod
  [_ message ctx e]
  (mark-hsx-syntax-error! ctx)
  ;; Let our React error boundary handle the exception - the error boundary is what our customers will see if we have broken the app...
  (throw (ex-info message ctx e)))
```

## Copyright and License

Copyright © 2021-2025 Factor House Pty Ltd.

Distributed under the Apache-2.0 License, the same as Apache Kafka.
