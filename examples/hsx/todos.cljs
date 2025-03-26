(ns hsx.todos
  (:require ["react" :as react]
            [clojure.string :as str]))

(defn class-names
  [& xs]
  (str/join " " (filter identity xs)))

(defn todo-input
  [add-todo]
  (let [[text set-text] (react/useState "")]
    [:form {:on-submit (fn [e]
                         (.preventDefault e)
                         (add-todo text)
                         (set-text ""))}
     [:input {:className   "border-2 border-gray-600 p-4 w-full outline-none text-xl placeholder-gray-300 rounded appearance-none"
              :placeholder ""
              :value       text
              :on-change   #(set-text (-> % .-target .-value))}]]))

(defn todo-item
  [{:keys [complete text datetime id]} remove-todo toggle-complete]
  [:div {:className "relative mb-4 p-3 shadow-md bg-white border rounded"}
   [:div {:className (class-names "text-2xl font-bold break-all"
                                  (if complete
                                    "text-gray-400 line-through"
                                    "text-indigo-600"))}
    text]
   [:div {:className "mt-2 text-gray-500 text-sm flex justify-between items-center text-xs"}
    [:div
     [:i {:className "fa fa-clock-o mr-1" :aria-hidden "true"}]
     (.toLocaleString datetime)]
    [:div
     [:button {:className (class-names "outline-none focus:outline-none"
                                       (when complete "text-green-600"))
               :on-click  #(toggle-complete id)}
      [:i {:className "fa fa-check fa-fw" :aria-hidden "true"}]]
     [:button {:className "outline-none focus:outline-none ml-4 focus:text-red-600"
               :on-click  #(remove-todo id)}
      [:i {:className "fa fa-trash fa-fw" :aria-hidden "true"}]]]]])

(defn todos
  []
  (let [[todos set-todos] (react/useState [])
        add-todo        (fn [text]
                          (when (not (str/blank? text))
                            (set-todos (conj todos {:id       (str (random-uuid))
                                                    :text     text
                                                    :complete false
                                                    :datetime (js/Date.)}))))
        remove-todo     (fn [id]
                          (->> todos
                               (filter #(not= id (:id %)))
                               (vec)
                               (set-todos)))

        toggle-complete (fn [id]
                          (->> todos
                               (mapv #(if (= id (:id %))
                                        (assoc % :complete (not (:complete %)))
                                        %))
                               (set-todos)))]

    [:div {:className "bg-gray-100 w-screen min-h-screen"}
     [:div {:className "container mx-auto p-4 max-w-md"}
      [:h1 {:className "p-4 text-gray-600 text-4xl font-bold text-center"}
       :todos]
      [todo-input add-todo]
      [:div {:className "mt-6"}
       (for [todo todos]
         ^{:key (:id todo)}
         [todo-item todo remove-todo toggle-complete])]]]))