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

(defn serve-print-jobs []
  (log/info "Starting server loop")
  (loop []
    (print-all-jobs)
    (let [connection (xmpp/connect (:xmpp-jid (cfg/configuration-parms)) (gcp/access-token))]
      (try
        (process-print-job-notifications connection)
        (catch javax.xml.stream.XMLStreamException e
          (xmpp/close connection)
          (log/warn "XML error " e " while serving job notifications"))
        (catch java.lang.Exception e
          (xmpp/close connection)
          (log/warn "caught exception " e " while serving job notifications"))))
    (util/sleep 3)
    (recur)))


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
