(ns gcpc.gcp
  (:require [gcpc.http :as http]
            [clj-time.core :as time]
            [gcpc.conf :as cfg]
            [gcpc.util :as util]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]))

(def client-id "the client-id you got from the Google API Console")
(def client-secret "the client-secret you got from the Google API Console")

(def oauth-url "https://accounts.google.com/o/oauth2/token")

(defn print-cloud-url
  ([]
   "https://www.google.com/cloudprint")
  ([path]
   (str (print-cloud-url) "/" path)))

;; every 5 seconds according to documentation
(def auth-poll-period 5)

(defn proxy-id []
  (str "gcpc@" (util/hostname)))

(defn fetch-access-token [refresh-token]
  (let [parms {:client_id client-id
               :client_secret client-secret
               :grant_type "refresh_token"
               :refresh_token refresh-token}]
    (-> (http/http-post oauth-url {:form-params parms})
        http/assert-http-success
        http/decode-json-body
        :body
        http/assert-json-success
        ((juxt :access-token :expires-in)))))

(defn update-access-token [conf]
  (dosync
   (let [atok (deref (:access-token conf))
         exp (deref (:expiration conf))
         rtok (:refresh-token conf)]
     (when (and rtok
                (or (not exp)
                    (time/before? exp (time/now))))
       (let [[atok' expin] (fetch-access-token rtok)]
         (ref-set (:access-token conf) atok')
         (ref-set (:expiration conf) (-> expin time/seconds time/from-now)))))
   conf))

(defn access-token []
  (-> (cfg/configuration-parms)
      update-access-token
      :access-token
      deref))

(defn add-gcp-headers
  "Add headers common to all GCP requests."
  [req]
  (let [req (update req :headers assoc "X-CloudPrint-Proxy" "gcpc")
        atok (access-token)]
    (if atok
      (assoc req :oauth-token atok)
      req)))

(defn gcp-req* [op url req]
  (-> (op url (add-gcp-headers req))
      http/assert-http-success
      http/decode-json-body
      :body))

(defn gcp-req
  "Just like `gcp-req*` but ensure authentication. OAuth2 credentials must be available."
  [op url req]
  (when-not (access-token)
    (throw (java.lang.Exception. "No access token.  Have you added at least a printer before?")))
  (gcp-req* op url req))

(defn gcp-register [name ppd]
  (let [parms {:name name
               :proxy (proxy-id)
               :capabilities ppd}]
    (gcp-req* http/http-post (print-cloud-url "register") {:form-params parms})))

(defn- wait-user-confirmation [url time]
  (let [end (-> time time/seconds time/from-now)]
    (loop [now (time/now)]
      (if (time/after? now end)
        nil
        (do
          (util/sleep auth-poll-period)
          (let [confirmation (gcp-req* http/http-get url {})]
            (if (get confirmation :success false)
              confirmation
              (recur (time/now)))))))))

(defn fetch-refresh-token [authorization-code]
  (let [parms {:redirect_uri "oob"
               :client_id client-id
               :client_secret client-secret
               :grant_type "authorization_code"
               :code authorization-code}]
    (-> (gcp-req* http/http-post oauth-url {:form-params parms})
        http/assert-json-success
        :refresh-token)))

(defn- complete-printer-registration [registration]
  ;; If it is the first printer we register, the user needs to
  ;; confirm the registration and, after that, we need to fetch a one-time
  ;; authorization code, which enables us to get an OAuth2 refresh
  ;; token.
  ;; That refresh token should be saved somehwere for later getting an
  ;; OAuth2 access token.  This access token is used to perform the
  ;; GCP requests.  The access token expires about every hour and a
  ;; new one needs to be requested with the refresh token.
  (when (:token-duration registration)
    (let [timeout (-> registration
                      :token-duration
                      bigint int)
          conf-url (str (:polling-url registration) client-id)
          confirmation (do (println "Please visit"
                                    (:complete-invite-url registration)
                                    "to claim printer"
                                    name)
                           (wait-user-confirmation conf-url timeout))]
      (if confirmation
        (merge (select-keys confirmation [:xmpp-jid :user-email])
               {:refresh-token (fetch-refresh-token (:authorization-code confirmation))})
        (throw (ex-info (format "No user confirmation after %d seconds" timeout) registration))))))

(defn register-printer [name ppd]
  (let [registration (gcp-register name ppd)]
    [(-> registration :printers first :id)
     (complete-printer-registration registration)]))

(defn list-printers []
  (let [answer (gcp-req http/http-post (print-cloud-url "list")
                        {:form-params {:proxy (proxy-id)}})]
    (if (:success answer)
      (:printers answer)
      (throw (ex-info "Cannot list printers" answer)))))

(defn list-jobs [printer-id]
  (try
    (:jobs (gcp-req http/http-post (print-cloud-url "fetch")
                    {:form-params {:printerid printer-id}}))
    (catch clojure.lang.ExceptionInfo e
      (let [result (ex-data e)]
        (if (and (not (:success result))
                 (= (:errorCode result) 413))
          []
          (throw e))))))

(defn list-all-jobs []
  (->> (list-printers)
       (mapcat (comp list-jobs :id))))

(defn change-job-state [job state]
  (let [answer (gcp-req http/http-post (print-cloud-url "control")
                        {:form-params (merge {:jobid (:id job)}
                                             state)})]
    (if (:success answer)
      answer
      (throw (ex-info (str "Cannot change job" job) answer)))))

(defn job-cancel [job]
  (change-job-state job {:status "ABORTED" :message "CANCELLED"}))

(defn job-in-progress [job]
  (change-job-state job {:status "IN_PROGRESS"}))

(defn job-completed [job]
  (change-job-state job {:status "DONE"}))

(defn job-failed [job]
  (change-job-state job {:status "ERROR"}))

(defn purge-dead-printers
  "Remove from local configuration those printers that do not appear
  configured in the cloud."
  []
  (let [live-ids (map :id (list-printers))]
    (cfg/save-configuration-parms
     (update (cfg/configuration-parms) :printers
             #(select-keys % live-ids)))))

(defn delete-printer [id]
  (let [answer (gcp-req http/http-post (print-cloud-url "delete")
                        {:form-params {:printerid id}})]
    (if (:success answer)
      (do (purge-dead-printers)
          true)
      (throw (ex-info (str "Cannot delete printer " id) (select-keys answer [:errorCode :message]))))))

(defn delete-all-printers []
  (run! (comp delete-printer :id) (list-printers)))

(defn get-job-body [job]
  (-> (http/http-get (:fileUrl job) (add-gcp-headers {:oauth-token (access-token)}))
      http/assert-http-success
      :body))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Protocol 2.0
;;;

(defn job-delete [job]
  (let [answer (gcp-req http/http-post (print-cloud-url "deletejob")
                        {:form-params {:jobid (:id job)}})]
    (if (:success answer)
      (do (purge-dead-printers)
          true)
      (throw (ex-info (str "Cannot delete print job " job) (select-keys answer [:errorCode :message]))))))

(defn change-job-state2 [job state]
  (let [answer (gcp-req http/http-post (print-cloud-url "control")
                        {:form-params {:jobid (:id job)
                                       :semantic_state_diff (json/encode state)}})]
    (if (:success answer)
      answer
      (throw (ex-info (str "Cannot change job" job) answer)))))
