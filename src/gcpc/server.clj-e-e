(ns gcpc.server
  (:require [gcpc.cups :as cups]
            [gcpc.gcp :as gcp]
            [gcpc.conf :as cfg]
            [gcpc.xmpp :as xmpp]
            [clojure.tools.logging :as log]))

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

(defn process-print-job-notifications [in]
  (doseq [printer-id (xmpp/notification-seq in)]
    (log/debug "jobs on printer" printer-id)
    (run! print-job (gcp/list-jobs printer-id))))

(defn print-all-jobs []
  (run! print-job (gcp/list-all-jobs)))

(defn serve-print-jobs []
  (print-all-jobs)
  (let [[input-channel socket] (xmpp/connect (:xmpp-jid (cfg/configuration-parms)) (gcp/access-token))]
    (process-print-job-notifications input-channel)))

