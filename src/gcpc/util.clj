(ns gcpc.util)

(defn merge-deep [& maps]
  (letfn [(mw [& maps] (apply merge-with mw maps))]
    (apply mw maps)))

(defn sleep [secs]
  (Thread/sleep (* 1000 secs)))

(def hostname
  (memoize #(.getCanonicalHostName (java.net.InetAddress/getLocalHost))))

(defn url? [thing]
  (instance? java.net.URL thing))

(defn as-url [thing]
  (if (url? thing)
    thing
    (java.net.URL. thing)))

(defn url-port [url]
  (let [p (.getPort url)]
    (if (pos? p)
      p
      nil)))

(defn url-host [url]
  (.getHost url))

(defn parse-host-name [host]
  (let [[match? host _ port] (re-matches #"([^:]+)(:([0-9]+))?" host)]
    (when match?
      [host port])))

(defn parse-printer-name [printer]
  (let [[match? name _ host _ port] (re-matches #"([^@]+)(@([^:]+)(:([0-9]+))?)?" printer)]
    (when match?
      {:name name
       :host host
       :port port})))
