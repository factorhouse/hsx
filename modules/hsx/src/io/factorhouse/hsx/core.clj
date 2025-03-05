(ns io.factorhouse.hsx.core)

(defmacro defcomponent
  [name args & body]
  `(def ~name
     (let [comp# (fn [props#]
                   (let [elem-args# (obj-get props# "args")
                         comp#      (let [~args elem-args#]
                                      (do ~@body))]
                     (create-element comp#)))]
       (set-display-name comp# ~(str name))
       (map->Component {:comp comp#
                        :memo (react-memo comp# are-props-equal?)}))))

(defmacro component
  [args & body]
  `(let [comp# (fn [props#]
                 (let [elem-args# (obj-get props# "args")
                       comp#      (let [~args elem-args#]
                                    (do ~@body))]
                   (create-element comp#)))]
     (set-display-name comp# ~(str (gensym "component")))
     (map->Component {:comp comp#
                      :memo (react-memo comp# are-props-equal?)})))
