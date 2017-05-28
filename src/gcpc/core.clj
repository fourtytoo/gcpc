(ns gcpc.core
  (:gen-class)
  (:require [gcpc.gcp :as gcp]
            [gcpc.conf :as cfg]
            [gcpc.server :as srv]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.pprint :refer [pprint]]
            [onelog.core :as log]
            [gcpc.cups :as cups]
            [gcpc.util :as util]))

(defn configure-printer
  "Configure CUPS printer `printer` on the cloud.  If this is the
first printer we register save the connection parameters locally."
  [printer]
  (println "adding" (cups/printer-url printer))
  (let [[id reg] (gcp/register-printer (cups/printer-name printer) (cups/printer-ppd printer))
        conf {id {:id id
                  :name (cups/printer-name printer)
                  ;; :host (cups/printer-host printer)
                  :url (str (cups/printer-url printer))}}]
    (cfg/save-configuration-parms
     (util/merge-deep (cfg/configuration-parms)
                      {:printers conf}
                      reg))
    conf))

(defn configure-printers
  "printer@host = specific printer at host,
  *@host = all printers on host,
  printer = local printer
  * = all local printers"
  [printers]
  (mapcat 
   (fn [printer]
     (let [printer' (util/parse-printer-name printer)
           client (cups/client :host (or (:host printer') cups/*default-cups-host*)
                               :port (or (:port printer') cups/*default-cups-port*))]
       (if (= "*" (:name printer'))
         (map configure-printer (cups/list-printers client))
         (-> printer'
             :name
             (cups/find-printer client)
             (or (throw (java.lang.Exception. (str "printer " printer " not found"))))
             configure-printer
             list))))
   printers))

(defn list-gcp-printers []
  (run! pprint (gcp/list-printers)))

(defn list-cups-printers [host port]
  (->> (cups/client :host host :port (or port cups/*default-cups-port*))
       cups/list-printers
       (run! (comp println str cups/printer-url))))

(defn list-printers
  "List CUPS printers on hosts.  If no host is specified, list all
  known printer configured on GCP."
  [hosts]
  (if (empty? hosts)
    (list-gcp-printers)
    (run! (comp (partial apply list-cups-printers)
                util/parse-host-name)
          hosts)))

(defn remove-jobs
  "Abort and delete jobs on Google Cloud Print.  Not those already
  queued on local CUPS hosts."
  [jobids]
  (doseq [id jobids]
    (gcp/job-cancel id)))

(defn remove-printers
  "Delete from configuration and the Google Cloud printers.  Not from
  the CUPS servers!
  id = specific printer with Google ID,
  printer = local printer,
  printer@host = specific printer on host,
  
  This command removes only GCP printers, not those configured on CUPS
  servers."
  [printers]
  (doseq [p printers]
    (println "deleting" p)
    (or (gcp/delete-printer p)
        (let [[name host] (util/parse-printer-name p)
              [id parms] (cfg/find-printer name host)]
          (gcp/delete-printer id)))))

(defn list-jobs []
  (doseq [j (gcp/list-all-jobs)]
    (println j)))

(def cli-options
  [["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ["-c" "--config FILE" "Configuration file instead of ~/.gcpc"]
   ["-h" "--help" "This usage screen"]])

(defn usage [options-summary]
  (->> ["Google Cloud Print Proxy."
        ""
        "Usage: gcpp [options] action [args]"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  add printer ...                 add printers"
        "  serve|start                     start serving cloud print jobs"
        "  printers|list-printers          list printers configured on the cloud"
        "  printers|list-printers host ... list CUPS printers on local hosts"
        "  rm|remove-job id ...            remove job from the cloud"
        "  remove-printer id|name@host ... remove printer from the cloud"
        "  jobs|list-jobs                  list print jobs cyrrently on the cloud"
        "

Printers can be specified just by name, in which case they must belong
to localhost, or with the syntax name@host.  You can also specify the
port number of the remote CUPS server (usually 631) after a colon like
name@host:631.

Print jobs already handed over to CUPS servers cannot be deleted by
this program; refer to lprm(1) or cups(1) manual.

This pogram configures/deletes only cloud printers.  To
configure/delete local CUPS printers check the cups(1) manual page. "]
       (clojure.string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (clojure.string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (log/start! "log/gcpc.log" :info)
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond
      (:help options) (exit 0 (usage summary))
      (< (count arguments) 1) (exit 1 (str "Too few arguments\n"
                                           (usage summary)))
      errors (exit 2 (str "Command line parsing error\n"
                          (error-msg errors))))
    ;; Execute program with options
    (binding [cfg/*configuration-file* (:config options)]
      (case (first arguments)
        "add" (let [l (configure-printers (rest arguments))]
                (run! println l)
                (println "added" (count l) "printers"))
        ("start" "serve") (srv/serve-print-jobs)
        ("printers" "list-printers") (list-printers (rest arguments))
        ("rm" "remove-job") (remove-jobs (rest arguments))
        ("remove-printer") (remove-printers (rest arguments))
        ("jobs" "list-jobs") (list-jobs)
        (exit 3 (str "Unknown action " (first arguments) "\n"
                     (usage summary)))))))

#_(binding [cfg/*configuration-file* "/home/wcp/fatcat/.gcpc"]
    (srv/serve-print-jobs))

#_(srv/serve-print-jobs)
