;; Please note:
;; The methods in this namespace are only intended for internal use.

(ns clarango.utilities.http-utility
	(:require [clj-http.client :as http]
		        [cheshire.core :refer :all])
  (:use [clojure.pprint]
        [clarango.utilities.core-utility :only [get-default-db get-default-collection-name]]
        [clarango.utilities.uri-utility :only [build-ressource-uri]]))

;;; debug switches:
(defn http-debugging-activated? []
  "Switch that activates the verbose output of clj-http."
  true)

(defn type-output-activated? []
  "Switch that activates outputting the type of the response."
  false)

(defn console-output-activated? []
  "Switch that activates outputting the http method and url used for each http request."
  true)

(defn- build-server-exception-string
  [error]
  (str "Clarango: There was an error trying to access a resource: " 
    (.getMessage error) 
    "; body: " 
    (parse-string (:body (:object (.getData error))))))

(defn- build-connection-exception-string
  [error]
  (str (.getMessage error)
    "; There is probably something wrong with the server. "
    "Is the server running and did you set the right connection-url?"))

(defmulti handle-error
  "Handle an error trying to access a resource."
  (fn [error] (type error)))
(defmethod handle-error java.net.ConnectException
  [error]
  (throw (Exception. (build-connection-exception-string error))))
(defmethod handle-error clojure.lang.ExceptionInfo
  [error]
  (throw (Exception. (build-server-exception-string error))))
(defmethod handle-error :default
  [error]
  (throw error))
;; TO DO: later create custom exceptions for Clarango?

(defn- get-uppercase-string-for-http-method
  "Returns a string of uppercase letters with the name of the matching http method.
  Pass it a method name as symbol, e.g. :post"
  [method]
  (case method
    :get "GET"
    :head "HEAD"
    :post "POST"
    :put "PUT"
    :patch "PATCH"
    :delete "DELETE"))

(defn- parse-if-possible
  "Parses a JSON string using the cheshire/parse-string function if possible. 
  If an error occurs, the string is returned as it is."
  [string]
  (try (parse-string string)
    (catch Exception e (do #_(println "parse error:   " string) string))))

(defn- incremental-keyword-lookup
  "Takes a map and an array of keywords and performs a nested lookup, meaning one keyword after another is used."
  [map keyword-vec]
  (loop [new-map map keywords keyword-vec]
    (if (empty? keywords) new-map (recur (parse-if-possible (get new-map (first keywords))) (rest keywords)))))

(defn- filter-response
  "Filters a HTTP response with given instruction. Also applies cheshires parse-string method where possible.

  Pass the response JSON as first argument.
  The second argument has to be a map with of the form:
  {:parse-string true/false :keywords [:keyword1 :keyword2...]}"
  [response filter-keys]
  (incremental-keyword-lookup response filter-keys))

(defn- send-request [method response-keys uri body params]
  (if (console-output-activated?) (println (get-uppercase-string-for-http-method method) " connection address: " uri))
  (try (let [ map-with-body (if (nil? body) {} {:body (generate-string body)})
              response (http/request (merge {:method method :url uri :debug (http-debugging-activated?) :query-params params} map-with-body))
              filtered-response (filter-response response response-keys)]
            (if (type-output-activated?) (println (type filtered-response)))
            filtered-response)
        (catch Exception e (handle-error e))))

(defn- build-multipart-vector
  "Builds an vector of the format:
  [{ :name 'who cares'
     :mime-type 'application/x-arango-batchpart'
     :encoding 'UTF-8'
     :content (str '\r\n\r\n' url '\r\n\r\n' (generate-string body))}
   { :name 'who cares'
     :mime-type 'application/x-arango-batchpart'
     :encoding 'UTF-8'
     :content 'String!'}]

  Takes a vector of the format: [{:uri 'http://someuri' :body body} ...]"
  [content-vector]
  (loop [content-vec content-vector new-content-vec []]
    (let [uri (:uri (first content-vec))
          body (:body (first content-vec))
          new-entity { :name "who cares"
                       :mime-type "application/x-arango-batchpart"
                       :encoding "UTF-8"
                       :content (str "\r\n\r\n" uri "\r\n\r\n" (generate-string body))}]
      (if (empty? (rest content-vec)) new-content-vec (recur (rest content-vec) (conj new-content-vec new-entity))))))

;; in development, not working yet!
(defn- send-multipart-request 
  "content is a vector of maps in the form [{:uri 'http://someuri' :body body} ...]"
  [response-keys uri content]
  (http/post uri {:content-type "multipart/form-data; boundary=XXXsubpartXXX"
                   :multipart (build-multipart-vector content)
                   :proxy-host "http://127.0.0.1" :proxy-port 3128}))

(defn get-uri 
  ([response-keys uri]
  (send-request :get response-keys uri nil nil))
  ([response-keys uri params]
  (send-request :get response-keys uri nil params)))

(defn head-uri 
  ([response-keys uri]
  (send-request :head response-keys uri nil nil))
  ([response-keys uri params]
  (send-request :head response-keys uri nil params)))

(defn delete-uri 
  ([response-keys uri]
  (send-request :delete response-keys uri nil nil))
  ([response-keys uri params]
  (send-request :delete response-keys uri nil params)))

(defn post-uri 
  ([response-keys uri]
  (send-request :post response-keys uri nil nil))
  ([response-keys uri body]
  (send-request :post response-keys uri body nil))
  ([response-keys uri body params]
  (send-request :post response-keys uri body params)))

(defn- build-content-map
  [bodies collection-name db-name]
  (let [uri (build-ressource-uri "document/?collection=" nil collection-name db-name)] ; please note: the uri is so far only applicable for creating new documents
    (loop [bodiesvec bodies content-map []]
      (let [content-map-new (conj content-map {:uri uri :body (first bodiesvec)})]
        (if (not (empty? bodiesvec)) (recur (rest bodiesvec) content-map-new) content-map-new)))))

(defn post-multi-uri
  [response-keys uri bodies collection-name db-name]
  (let [content (build-content-map bodies collection-name db-name)]
    (send-multipart-request response-keys uri content)))

(defn put-uri 
  ([response-keys uri]
  (send-request :put response-keys uri nil nil))
  ([response-keys uri body]
  (send-request :put response-keys uri body nil))
  ([response-keys uri body params]
  (send-request :put response-keys uri body params)))

(defn patch-uri 
  ([response-keys uri]
  (send-request :patch response-keys uri nil nil))
  ([response-keys uri body]
  (send-request :patch response-keys uri body nil))
  ([response-keys uri body params]
  (send-request :patch response-keys uri body params)))