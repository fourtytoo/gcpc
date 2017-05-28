(ns gcpc.xmpp
  (:require [less.awful.ssl :as ssl]
            [gcpc.xml :as xml]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            [clj-time.core :as time]
            #_[clojure.tools.logging :as log]
            [onelog.core :as log]
            [clojure.core.async :as async]
            [clojure.data.zip.xml]
            [clojure.xml]
            [clojure.data.xml :as dxml])
  #_(:import (java.net Socket)))

(def ^:dynamic *xmpp-server* "talk.google.com")
(def ^:dynamic *xmpp-port* 5223)

(defn- make-dummy-trust-manager []
  (proxy [javax.net.ssl.X509TrustManager][]
    (getAcceptedIssuers []
      (into-array java.security.cert.X509Certificate []))
    (checkClientTrusted [chain auth-type])
    (checkServerTrusted [chain auth-type])))

(def ssl-context-factory
  (let [ctx (javax.net.ssl.SSLContext/getInstance "TLS")
        tms (into-array javax.net.ssl.X509TrustManager [(make-dummy-trust-manager)])]
    (.init ctx nil tms nil)
    ctx))

(defn- create-insecure-ssl-engine []
  (.createSSLEngine ssl-context-factory))

(defn socket-write [out string]
  (.write out (.getBytes string))
  (.flush out))

(defn- b64-encode [string]
  (-> string
      .getBytes
      b64/encode
      (String. "UTF-8")))

(defn- b64-decode [string]
  (-> string
      .getBytes
      b64/decode
      (String. "UTF-8")))

(defn socket-reader [socket c]
  (async/go
    (doseq [e (first (xml/socket-xml-element-seq socket #(= :stream (.name %))))]
      (async/>! c e))))

(defn read-stanza [c]
  (let [[stanza chan] (async/alts!! [c (async/timeout (* 10 1000))])]
    (when (= c chan)
      stanza)))

(defn- jid-from-stanza [stanza]
  (-> stanza
      clojure.zip/xml-zip
      (clojure.data.zip.xml/xml1-> :iq :bind :jid clojure.data.zip.xml/text)))

(defn- bare-jid-from-stanza [stanza]
  (-> stanza
      jid-from-stanza
      (clojure.string/split #"/")
      first))

(defn- features-from-stanza [stanza]
  (map :tag (-> stanza
                clojure.zip/xml-zip
                (clojure.data.zip.xml/xml-> :features clojure.zip/children))))

(defn- iq-result-from-stanza [stanza]
  (-> stanza
      clojure.zip/xml-zip
      (clojure.data.zip.xml/xml1-> :iq (clojure.data.zip.xml/attr= :type "result") clojure.zip/node)
      :attrs))

(defn- auth-methods-from-stanza [stanza]
  (-> stanza
      clojure.zip/xml-zip
      (clojure.data.zip.xml/xml-> :features :mechanisms :mechanism clojure.data.zip.xml/text)))

(defn- subscribe-to-notifications [in out jid]
  (socket-write out "<iq type=\"set\" id=\"1\"><session xmlns=\"urn:ietf:params:xml:ns:xmpp-session\"/></iq>")
  (let [stanza (read-stanza in)
        result (iq-result-from-stanza stanza)]
    (when-not (= (:id result) "1")
      (throw (ex-info "unexpected session result" stanza))))
  (socket-write out (str "<iq type=\"set\" id=\"3\" to=\"" jid "\">"
                         "<subscribe xmlns=\"google:push\">"
                         "<item channel=\"cloudprint.google.com\" from=\"cloudprint.google.com\"/>"
                         "</subscribe>"
                         "</iq>"))
  (let [stanza (read-stanza in)
        result (iq-result-from-stanza stanza)]
    (when-not (= (:id result) "3")
      (throw (ex-info "unexpected subscribe result" stanza)))))

(defn- bind-resource [in out]
  (socket-write out "<iq type=\"set\" id=\"0\"><bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\"><resource>gcpc</resource></bind></iq>")
  (let [stanza (read-stanza in)
        bare-jid (bare-jid-from-stanza stanza)]
    (log/debug "bare jid:" bare-jid)
    (when (nil? bare-jid)
      (throw (ex-info "missing jid" stanza)))
    bare-jid))

(defn connect [jid token]
  (let [auth (-> (str "\000" jid "\000" token)
                 b64-encode)
        in (async/chan)
        socket (ssl/socket ssl-context-factory *xmpp-server* *xmpp-port*)
        out (.getOutputStream socket)]
    (async/thread (socket-reader socket in))
    (socket-write out "<stream:stream to=\"gmail.com\" xml:lang=\"en\" version=\"1.0\" xmlns:stream=\"http://etherx.jabber.org/streams\" xmlns=\"jabber:client\">\n")
    (let [stanza (read-stanza in)
          methods (auth-methods-from-stanza stanza)]
      (when-not (some #(= % "X-OAUTH2") methods)
        (throw (ex-info "XMPP server does not support X-OAUTH2 mechanism" stanza))))
    (socket-write out (str "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"X-OAUTH2\">" auth "</auth>\n"))
    (let [stanza (read-stanza in)]
      (when-not (= :success (:tag stanza))
        (throw (ex-info "Unexpected answer" stanza))))
    (socket-write out "<stream:stream to=\"gmail.com\" xml:lang=\"en\" version=\"1.0\" xmlns:stream=\"http://etherx.jabber.org/streams\" xmlns=\"jabber:client\">\n")
    (let [stanza (read-stanza in)
          features (features-from-stanza stanza)]
      (when-not (some #(= % :bind) features)
        (throw (ex-info "missing :bind in features" stanza))))
    (->> (bind-resource in out)
         (subscribe-to-notifications in out))
    [in socket]))

(defn- push-notification-from-stanza [stanza]
  (-> stanza
      clojure.zip/xml-zip
      (clojure.data.zip.xml/xml1-> :message :push :data clojure.data.zip.xml/text)
      b64-decode))

(defn notification-seq [in]
  (remove nil?
          (repeatedly
           (fn []
             (when-let [stanza (read-stanza in)]
               (push-notification-from-stanza stanza))))))
