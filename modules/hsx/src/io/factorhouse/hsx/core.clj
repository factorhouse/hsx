(ns io.factorhouse.hsx.core)

(defmacro component
  "Macro used to create a named Hsx component:

  (def c (component :MyComponent (constantly [:div \"Hello world\"])))"
  [display-name component-f]
  `(let [comp#         (fn ~(symbol display-name) [props#]
                         (let [elem-args# (obj-get props# "args")
                               comp#      (~component-f elem-args#)]
                           (create-element comp#)))
         display-name# ~(name display-name)]
     (set-display-name comp# display-name#)
     (map->Component {:proxy        comp#
                      :display-name display-name#
                      :proxy-memo   (react-memo comp# are-props-equal?)})))
