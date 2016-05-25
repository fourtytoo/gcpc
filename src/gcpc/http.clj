(ns gcpc.http
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [org.httpkit.client :as http]
            [gcpc.conf :as cfg]))

(defn http-success? [http-reply]
  (<= 200 (:status http-reply) 299))

(defn http-get [& args]
  (deref (apply http/get args)))

(defn http-post [& args]
  (deref (apply http/post args)))

(defn json-key->keyword [string]
  (-> string
      (clojure.string/replace "_" "-")
      keyword))

(defn json-decode [string]
  (json/parse-string string json-key->keyword))

(defn parse-content-type [string]
  (let [[type & options] (clojure.string/split string #"\s*;\s*")]
    [type (->> options
               (map (partial re-matches #"\s*charset\s*=\s*([^\s]+).*"))
               (remove nil?)
               first second)]))

(defn decode-body [http-response]
  (let [[content-type charset] (-> http-response
                                   (get-in [:headers :content-type])
                                   parse-content-type)]
    (update http-response :body
            (fn [body]
              ((case content-type
                 "application/json" json-decode
                 identity)
               body)))))

(defn decode-json-body [http-response]
  (update http-response :body json-decode))

(defn assert-http-success [http-response]
  (if (http-success? http-response)
    http-response
    (throw (ex-info "HTTP request failed" http-response))))

(defn assert-json-success [body]
  (if (and (map? body)
           (get body :success true))
    body
    (throw (ex-info "JSON error" body))))
