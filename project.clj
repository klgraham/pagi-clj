(defproject pagi-clj "0.1.0-SNAPSHOT"
  :description "A Clojure library for working with PAGI documents and schema."
  :url "http://example.com/FIXME"
  ;:license {:name "Eclipse Public License"
  ;          :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [speclj "3.3.2"]]
  :plugins [[speclj "3.3.0"]]
  ;:main ^:skip-aot pagi-clj.core
  ;:target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[speclj "3.3.0"]]}}

  :resource-paths ["src/resources/pagi_clj"]
  :test-paths ["test"])
