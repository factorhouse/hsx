(ns hsx.app
  (:require ["react" :as react]
            ["react-dom/client" :as react-dom]
            [hsx.clock :as clock]
            [hsx.todo-mvc :as todo-mvc]
            [io.factorhouse.hsx.core :as hsx]))

(defn app []
  (let [[example set-example] (react/useState "clock")]
    (prn example)
    [:<>
     [:nav {:className "bg-white border-gray-200 dark:bg-gray-900 dark:border-gray-700"}
      [:div {:className "max-w-screen-xl flex flex-wrap items-center justify-between mx-auto p-4"}
       [:select {:value     example
                 :on-change #(set-example (-> % .-target .-value))}
        [:option {:value "clock" :label "Clock"}]
        [:option {:value "todo-mvc" :label "Todo Mvc"}]]]]
     (case example

       "clock"
       [clock/clock]

       "todo-mvc"
       [todo-mvc/todo-mvc])]))

(defonce root
         (react-dom/createRoot (.getElementById js/document "app")))

(defn init []
  (.render root (hsx/create-element [app])))