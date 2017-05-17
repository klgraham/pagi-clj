(ns pagi-clj.schema
  (:require [clojure.spec.alpha :as s])
  (:use [clojure.xml :only (parse)]))

;;;; Loads a PAGI schema XML spec and generates functions to construct and
;;;; process the node types specified in the XML file

(defn load-schema [path-to-file]
  "Loads a PAGI schema from an XML file"
  (if (s/valid? string? path-to-file)
    (:content (parse (java.io.File. path-to-file)))))

(defn get-node-specs [schema]
  "Returns a sequence of the node type definitions in the schema"
  (s/valid? map? schema)
  (let [is-node-type? #(= (:tag % nil) :nodeType)]
    (filter is-node-type? schema)))

; all span nodes will have a :first and :last
(defn is-span? [node]
  "True if node is a span"
  (let [tags (:content node)
        has-span-tag? (not (empty? (filter #(= (:tag % nil) :span) tags)))]
    has-span-tag?))

; All sequence nodes will have a :next
(defn is-sequence? [node]
  "True if node is a sequence"
  (let [tags (:content node)
        has-sequence-tag? (not (empty? (filter #(= (:tag % nil) :sequence) tags)))]
    has-sequence-tag?))

(defrecord NamedProperty [type name attributes])

(defn get-node-properties [node property-type]
  "Returns properties of specified type, if present. Returns nil otherwise"
  (assert (s/valid? keyword? property-type) "Propery type must be a valid keyword.")
  (let [tags (:content node)
        prop-list (not-empty (filter #(= (:tag % nil) property-type) tags))
        properties (map :attrs prop-list)
        named-properties (map #(->NamedProperty property-type (:name %) (dissoc % :name)) properties)]
    (vec named-properties)))

(defn get-node-type [node]
  "Return the type of the node, e.g. TOK or EXTRACTOR"
  (-> node :attrs :name))

(defn create-node [is-sequence is-span
                   string-properties int-properties float-properties])

(defn create-node-generator [node]
  "Given a node specification, generate a function to construct node. Node are
  specified like:

  {
  :tag :nodeType
  :attrs {:idGenerator \"{prop:start}:{prop:length}\", :name \"TOK\"}
  :content [{:tag :sequence, :attrs nil, :content nil}
            {:tag :span, :attrs nil, :content nil}
            {:tag :stringProperty, :attrs {:maxArity \"1\", :minArity \"1\", :name \"prov\"}, :content nil}
            {:tag :floatProperty, :attrs {:maxRange \"1.0\", :minRange \"0.0\", :maxArity \"1\", :minArity \"0\", :name \"conf\"}, :content nil}
            {:tag :floatProperty, :attrs {:maxRange \"1.0\", :minRange \"0.0\", :maxArity \"1\", :minArity \"0\", :name \"margin\"}, :content nil}
            {:tag :edgeType, :attrs {:maxArity \"1\", :minArity \"0\", :targetNodeType \"RANKED_PREDICTION\", :name \"rankEdge\"}, :content nil}]"

  )