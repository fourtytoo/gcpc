(ns gcpc.server
  (:require [gcpc.cups :as cups]
            [gcpc.gcp :as gcp]
            [gcpc.conf :as cfg]
            [gcpc.xmpp :as xmpp]
            #_[clojure.tools.logging :as log]
            [onelog.core :as log]
            [gcpc.util :as util]))

(defn print-job [job]
  (let [url (-> job :printerid cfg/find-printer :url)
        title (:title job)
        user (:ownerId job)]
    (log/info  "job" (:id job) (str (:numberOfPages job) "p") title "->" url)
    (let [result (cups/print-stream (cups/find-printer-by-url url)
                                  (gcp/get-job-body job)
                                  :title title
                                  :user user)]
      (if (cups/success? result)
        (gcp/job-completed job)
        (gcp/job-failed job)))))

(defn process-print-job-notifications [connection]
  (doseq [printer-id (xmpp/notification-seq connection)]
    (log/debug "jobs on printer" printer-id)
    (run! print-job (gcp/list-jobs printer-id))))

(defn print-all-jobs []
  (run! print-job (gcp/list-all-jobs)))

(def ^:dynamic *reconnect-delay*
  "How long, in seconds, to wait before trying and reconnect to the GCP service."
  3)

(defn serve-print-jobs []
  (log/info+ "Starting server loop")
  (let [connect (fn []
                  (xmpp/connect (:xmpp-jid (cfg/configuration-parms))
                                (gcp/access-token)))]
    (loop [connection (connect)]
      (try
        (print-all-jobs)
        (process-print-job-notifications connection)
        (catch javax.xml.stream.XMLStreamException e
          (xmpp/close connection)
          (log/error "XML error " e))
        (catch java.lang.Exception e
          (xmpp/close connection)
          (log/error "Caught exception " e)))
      (util/sleep *reconnect-delay*))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defonce server (atom nil))

(defn debug-server [& [conf-file]]
  (log/start! "log/gcpc.debug" :debug)
  (swap! server
         (fn [thread]
           (when thread
             (future-cancel thread))
           (future
             (binding [cfg/*configuration-file* (or conf-file cfg/*configuration-file*)]
               (serve-print-jobs))))))

#_(debug-server)
