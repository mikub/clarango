(ns clarango.core)

(def clarango-connection nil)

(defn set-connection!
  "Connects permanently to an ArangoDB host by setting the connection map as a global variable.
  If called without arguments set default connection at localhost:8529 with _system db."
  ([]
    (def clarango-connection {:connection-url "http://localhost:8529/", :db-name "_system"}))
  ([connection-map]
    (def clarango-connection connection-map)))

(defn get-connection
  "Returns the db server connection map to other namespaces."
  []
  clarango-connection)

(defn connection-set?
  "Returns true if a connection is set."
  []
  (not (nil? clarango-connection)))

(defn- change-value-in-connection!
  [key value]
  (let [connection-map (if (connection-set?) (get-connection) (hash-map))]
    (set-connection! (assoc connection-map key value))))

(defn set-connection-url!
  "Sets the server url."
  [connection-url]
  (change-value-in-connection! :connection-url connection-url))

(defn set-default-db!
  "Sets a default database."
  [database-name]
  (change-value-in-connection! :db-name database-name))

(defn set-default-collection!
  "Sets a default collection."
  [collection-name]
  (change-value-in-connection! :collection-name collection-name))