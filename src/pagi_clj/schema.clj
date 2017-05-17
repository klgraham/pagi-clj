(ns pagi-clj.schema
  (:require [clojure.spec.alpha :as s])
  (:use [clojure.xml :only (parse)]))

;;;; Loads a PAGI schema XML spec and generates functions to construct and
;;;; process the node types specified in the XML file

; all span nodes will have a :start and :length to reference the star and end
; offsets into the source text
(defn is-span? [node]
  "True if node is a span"
  (let [tags (:content node)
        has-span-tag? (not (empty? (filter #(= (:tag % nil) :span) tags)))]
    has-span-tag?))

; all span container nodes will have a :first and :last
(defn is-span-container? [node]
  "True if node is a span container"
  (let [tags (:content node)
        has-span-container-tag? (not (empty? (filter #(= (:tag % nil) :spanContainer) tags)))]
    has-span-container-tag?))

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
        ; prop-list is a list of all the properties of the specified type
        prop-list (not-empty (filter #(= (:tag % nil) property-type) tags))
        attributes (map :attrs prop-list)
        named-properties (map #(->NamedProperty property-type (:name %) (dissoc % :name)) attributes)]
    (into [] named-properties)))

(defn get-enum-properties [node]
  "Returns enum properties, if present. Returns nil otherwise"
  (let [tags (:content node)
        ; properties is a list of all the properties of the enum
        properties (not-empty (filter #(= (:tag % nil) :enumProperty) tags))
        attributes (-> properties first :attrs)
        get-nested-enum #(-> % :attrs :name)
        enums (->> properties
                   first
                   :content
                   rest
                   (map get-nested-enum)
                   (into #{}))]
    (if (empty? enums)
      []
      [(->NamedProperty :enumProperty
                       (:name attributes)
                       (assoc (dissoc attributes :name) :enums enums))]) ))

(defrecord EdgeSpec [target-node-type min-arity max-arity])

(defn create-edge-spec [edge-def]
  (let [{target-node-type :targetNodeType name :name min-arity :minArity max-arity :maxArity} edge-def]
    {:name name :edge (->EdgeSpec target-node-type min-arity max-arity)}))

(defn get-edge-specs [node]
  (let [tags (:content node)
        edge-specs (not-empty (filter #(= (:tag % nil) :edgeType) tags))
        edge-specs (map #(create-edge-spec (:attrs %)) edge-specs)]
    edge-specs))

(defn get-node-type [node]
  "Return the type of the node, e.g. TOK or EXTRACTOR"
  (-> node :attrs :name))

(defn get-id-generator [node]
  (-> node :attrs :idGenerator))

(defn node-spec-generator [node]
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

  {:is-span (is-span? node)
   :is-span-container (is-span-container? node)
   :is-sequence (is-sequence? node)
   :node-type          (get-node-type node)
   :id-generator (get-id-generator node)
   :string-properties  (get-node-properties node :stringProperty)
   :integer-properties (get-node-properties node :integerProperty)
   :float-properties   (get-node-properties node :floatProperty)
   :boolean-properties (get-node-properties node :booleanProperty)
   :enum-properties    (get-enum-properties node)
   :edges              (get-edge-specs node)})

(defn create-node-specs [node-defs]
  (into {}
        (map #(hash-map (get-node-type %) (node-spec-generator %)) node-defs)))

(defn load-schema-file [path-to-file]
  "Loads a PAGI schema from an XML file"
  (if (s/valid? string? path-to-file)
    (:content (parse (java.io.File. path-to-file)))))

(defn get-node-schema [schema]
  "Returns a sequence of the node type definitions in the schema"
  (s/valid? map? schema)
  (let [is-node-type? #(= (:tag % nil) :nodeType)]
    (filter is-node-type? schema)))

(defn load-schema [path-to-file]
  "Loads a PAGI schema from an XML file"
  (let [schema (load-schema-file path-to-file)
        node-specs (get-node-schema schema)]
    (create-node-specs node-specs)))