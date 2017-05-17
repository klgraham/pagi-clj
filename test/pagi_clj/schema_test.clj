(ns pagi-clj.schema-test
  (:require [pagi-clj.schema :refer :all]
            [speclj.core :refer :all]))

(describe "Schema Loader"
          (def test-schema (load-schema-file "resources/pagi_clj/docgraph.xml"))

          (it "should be able to load a schema from disk"
              (should (not (nil? test-schema)))))