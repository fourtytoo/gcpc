(ns gcpc.conf
  (:require [gcpc.joda :as joda]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [gcpc.conf :as cfg]))


(defn home-directory []
  (System/getProperty "user.home"))

(def ^:dynamic *configuration-file*
  "Pathname of the configuration file.  If nil, it defaults to ~/.gcpc"
  nil)

(defn configuration-file []
  (io/file (or *configuration-file*
               (str (home-directory) "/.gcpc"))))

(defn file-exists? [file]
  (.exists (io/file file)))

(defn load-configuration-parms []
  (merge {:expiration (ref nil)
          :access-token (ref nil)}
   (if (file-exists? (configuration-file))
     (joda/with-joda-time-reader
       (with-open [stream (java.io.PushbackReader. (io/reader (configuration-file)))]
          (edn/read stream)))
     {})))

(defn save-configuration-parms [parms]
  (with-open [out (io/writer (configuration-file))]
    (.write out ";;; -*- Clojure -*-\n\n")
    (-> parms
        (dissoc :expiration)
        (dissoc :access-token)
        (clojure.pprint/pprint out))))

(defn file-modification-time [file]
  (.lastModified (io/file file)))

(let [state (atom [nil nil])]
  (defn configuration-parms
    ([id]
     (-> (configuration-parms)
         :printers
         (get id)))
    ([]
     (second
      (swap! state (fn [[epoch parms]]
                     (let [modtime (file-modification-time (configuration-file))]
                       (if (or (not epoch)
                               (> modtime epoch))
                         (let [parms' (load-configuration-parms)]
                           [modtime parms'])
                         [epoch parms]))))))))

(defn find-printer
  "Find a printer either by Google ID or by host+name."
  ([name host]
   (->> (configuration-parms)
       :printers
       (filter (fn [[id conf]]
                 (and (= (:name conf) name)
                      (= (:host conf) host))))
       first))
  ([id]
   (-> (configuration-parms)
       :printers
       (get id))))
