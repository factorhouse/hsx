(ns reagent.core
  "A compatibility namespace aliasing io.factorhouse.hsx.core/* as reagent.core/*

  Intended to assist with migrations of large codebases off of Reagent

  Shout outs to Reagent: https://github.com/reagent-project/reagent"
  (:require [io.factorhouse.hsx.core :as hsx]
            ["react" :as react]))

(defn as-element hsx/create-element)
(def reactify-component hsx/reactify-component)
(def create-element react/createElement)