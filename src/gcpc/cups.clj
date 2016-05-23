(ns gcpc.cups
  (:require [clojure.java.io :as io]
            [gcpc.util :as util])
  (:import (org.cups4j CupsClient PrintJob PrintJob$Builder)))

(def ^:dynamic *default-cups-host* org.cups4j.CupsClient/DEFAULT_HOST)
(def ^:dynamic *default-cups-port* org.cups4j.CupsClient/DEFAULT_PORT)

(def make-client
  (memoize
   (fn [host port]
     (org.cups4j.CupsClient. host port))))

(defn client [& {:keys [host port]
                 :or {host *default-cups-host*
                      port *default-cups-port*}}]
  (make-client host port))

(defn list-printers
  ([] (list-printers (client)))
  ([client]
   (.getPrinters client)))

(defn default-printer
  ([]
   (default-printer (client)))
  ([client]
   (.getDefaultPrinter client)))

(defn printer-description [printer]
  (.getDescription printer))

(defn printer-location [printer]
  (.getLocation printer))

(defn printer-name [printer]
  (.getName printer))

(defn printer-url [printer]
  (.getPrinterURL printer))

(defn printer-ppd [printer]
  (slurp (io/reader (str (printer-url printer) ".ppd"))))

(def ^:dynamic *default-print-job-owner* "gcpc")
(def ^:dynamic *default-print-job-title* "untitled")

(defn print-stream
  "Print tho whole content of `stream` on `printer`."
  [printer stream & {:keys [title copies user]
                     :or {title *default-print-job-title*
                          copies 1
                          user *default-print-job-owner*}}]
  (.print printer
          (-> stream
              org.cups4j.PrintJob$Builder.
              (.copies copies)
              (.userName user)
              (.jobName title)
              .build)))

(defn print-file [printer file & {:keys [title copies user] :as opts}]
  (apply print-stream printer
         (-> file
             io/file
             io/input-stream)
         opts))

(defn find-printer
  ([name] (find-printer name (client)))
  ([name client]
   (->> client
        list-printers
        (filter (comp (partial = name)
                      (partial printer-name)))
        first)))

(defn find-printer-by-url
  ([url]
   (let [url (util/as-url url)
         client (client :host (util/url-host url)
                        :port (or (util/url-port url) *default-cups-port*))]
     (find-printer-by-url url client)))
  ([url client]
   (.getPrinter client (util/as-url url))))

(defn printer-host [printer]
  (-> printer
      printer-url
      util/url-host))

(defn printer-port [printer]
  (-> printer
      printer-url
      util/url-port
      (or *default-cups-port*)))

(defn success? [result]
  (.isSuccessfulResult result))
