(ns hsx.app
  (:require ["react" :as react]
            ["react-dom/client" :as react-dom]
            [hsx.clock :as clock]
            [hsx.todos :as todos]
            [io.factorhouse.hsx.core :as hsx]))

(defn app []
  (let [[example set-example] (react/useState "clock")]
    [:<>
     [:nav {:className "bg-white border-gray-200 dark:bg-gray-900 dark:border-gray-700"}
      [:div {:className "max-w-screen-xl flex flex-wrap items-center justify-between mx-auto p-4"}
       [:select {:value     example
                 :on-change #(set-example (-> % .-target .-value))}
        [:option {:value "clock" :label "Clock"}]
        [:option {:value "todos" :label "Todos"}]]]]
     (case example
       "clock"
       [clock/clock]
       "todos"
       [todos/todos])]))

(defonce root (react-dom/createRoot (.getElementById js/document "app")))

(defn init []
  (.render root (hsx/create-element [app])))