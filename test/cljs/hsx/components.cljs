(ns hsx.components
  (:require [io.factorhouse.hsx.core :as hsx]))

(defn button-reagent-args
  [value on-click]
  [:button {:on-click on-click} value])

(defn button
  [{:keys [onClick children]}]
  [button-reagent-args children onClick])

(def Button
  (hsx/reactify-component button))

(defn button-via-react-elem
  [{:keys [onClick children]}]
  [:> Button {:on-click onClick} children])

(def ButtonViaReactElem
  (hsx/reactify-component button-via-react-elem))

(def FragmentedButton
  (hsx/reactify-component
    (fn [{:keys [onClick buttonOneValue buttonTwoValue]}]
      [:div
       [:<>
        [button {:onClick onClick :children buttonOneValue}]
        [button {:onClick onClick :children buttonTwoValue}]]])))

(def SeqButton
  (hsx/reactify-component
    (fn [{:keys [onClick buttonOneValue buttonTwoValue]}]
      (let [buttons [[button {:onClick onClick :children buttonOneValue}]
                     [button {:onClick onClick :children buttonTwoValue}]]]
        [:div
         (for [button buttons]
           ^{:key (str "button-" (-> button second :children))}
           button)]))))