<img src="https://travis-ci.org/edlich/clarango.png?branch=master" alt="travis-ci.org Build Status" title="Build Status" align="right" />
Clarango: a Clojure driver for ArangoDB
========

work in progress... please contact the two repository owners

The expected roadmap is:
* Late 2014: stable 1.0 with management / admin, graph, ... and all the features exposed by the REST API.

## Features

* various options for connecting
* document CRUD including various options
  -> for documentation on this see [document.clj](https://github.com/edlich/clarango/blob/master/src/clarango/document.clj)
* querying by example
* AQL queries (see [query namespace](https://github.com/edlich/clarango/blob/master/src/clarango/query.clj))
* collection management (see [collection namespace](https://github.com/edlich/clarango/blob/master/src/clarango/collection.clj))
* database management (see [database namespace](https://github.com/edlich/clarango/blob/master/src/clarango/database.clj))
* simple exception handling

* experimental clojure idiomatic collection methods like `cla-assoc!` and `cla-conj!` (see [collection_ops.clj](https://github.com/edlich/clarango/blob/master/src/clarango/collection_ops.clj) for details)

...

## Installation

The driver is hosted on [Clojars](https://clojars.org/clarango). Add this Leiningen dependency to your project.clj:
```
[clarango "0.2.2"]
```
Then require the lib in your clojure file. For example:
``` Clojure
(:require [clarango.core :as clarango.core]
			[clarango.document :as document])
```

## Usage

Setting the databse connection and getting a document by existing key:

```clojure
(clarango.core/set-connection! {:connection-url "http://localhost:8529/"})
(clojure.pprint (document/get-by-key "document-key" "my-collection" "my-db"))

;; or

(clarango.core/set-connection! 
  {
    :connection-url "http://localhost:8529/"
    :db-name "my-db"
    :collection-name "my-collection"
  })
(clojure.pprint (document/get-by-key "document-key"))

;; or

(clarango.core/set-connection! {:connection-url "http://localhost:8529/"})
(clarango.core/set-default-db! "my-db")
(clojure.pprint (document/get-by-key "document-key" "my-collection"))

;; or

;; set default parameters: standart db and port 8529 on localhost
(clarango.core/set-connection!)
(clojure.pprint (document/get-by-key "document-key" "my-collection"))
```

create/replace/update/delete document:

```clojure
(let [_ (clarango.core/set-connection! {
        :connection-url "http://localhost:8529/"
        :db-name "my-db"
        :collection-name "my-collection" })
      document {:name "awesome name" :city "where is he from?"}
      ;; create document
      result-doc (document/create document)
      new-key (get result-doc "_key")]
  (clojure.pprint result-doc)

  ;; replace document
  (let [document-new {:name "even more awesome name" :city "from Berlin of course"}]
    (clojure.pprint (document/replace-by-key document-new new-key)))

  ;; update document
  (let [document-update {:age "He's already 100 years old."}]
    (clojure.pprint (document/update-by-key document-update new-key)))

  ;; delete document
  (document/delete-by-key new-key))
```

## Feedback

...

## License

Licensed under the [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
