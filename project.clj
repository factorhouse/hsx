(defproject io.factorhouse/hsx "0.1.20"
  :description "HSX is a ClojureScript library for writing React components using Hiccup syntax."
  :url "http://github.com/factorhouse/hsx"
  :license {:name         "Apache-2.0 License"
            :url          "https://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo
            :comments     "same as Kafka"}
  :dependencies [[org.clojure/clojure "1.12.0" :scope "provided"]
                 [org.clojure/clojurescript "1.11.132" :scope "provided"]]
  :profiles {:dev   {:resource-paths ["dev-resources"]
                     :plugins        [[dev.weavejester/lein-cljfmt "0.13.0"]]
                     :dependencies   [[org.slf4j/slf4j-api "2.0.16"]
                                      [ch.qos.logback/logback-classic "1.3.14"]
                                      [cheshire "5.13.0" :exclusions [com.fasterxml.jackson.core/jackson-databind]]
                                      [clj-kondo "2025.01.16" :exclusions [com.cognitect/transit-java javax.xml.bind/jaxb-api]]]}
             :smoke {:pedantic? :abort}}
  :test-paths ["test/cljs"]
  :source-paths ["src"]
  :repositories [["github" {:url      "https://maven.pkg.github.com/factorhouse/hsx"
                            :username "private-token"
                            :password :env/GITHUB_TOKEN}]]
  :aliases {"kondo"  ["with-profile" "+smoke" "run" "-m" "clj-kondo.main" "--lint" "src" "test/cljs"]
            "fmt"    ["with-profile" "+smoke" "cljfmt" "check"]
            "fmtfix" ["with-profile" "+smoke" "cljfmt" "fix"]})
