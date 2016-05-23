;; NOTE:
;;
;; cups4j must be copied inside the directory ./resources,
;; then it needs to be installed in make_repository:
;; 
;; mkdir maven_repository
;; mvn install:install-file -Dfile=resources/cups4j.runnable-0.6.4.jar -DartifactId=cups4j -Dversion=0.6.4 -DgroupId=local -Dpackaging=jar -DlocalRepositoryPath=maven_repository -DcreateChecksum=true



(defn directory [pathname]
  (map str (.listFiles (clojure.java.io/file pathname))))

(defproject gcpc "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]
                 #_[clj-http-lite "0.3.0"]
                 [http-kit "2.1.18"]
                 #_[clj-http "3.0.1"]
                 [clj-time "0.11.0"]
                 #_[danlentz/clj-uuid "0.1.6"]
                 ;; see below
                 [local/cups4j "0.6.4"]
                 ;; necessary for cups4j
                 [org.apache.httpcomponents/httpclient "4.0.1"]
                 #_[xmpp-clj "0.3.1"]
                 [less-awful-ssl "1.0.1"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.2"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/tools.cli "0.3.5"]]
  :resource-paths ["resources/cups4j.runnable-0.6.4.jar"]
  :repositories {"local" ~(str (.toURI (java.io.File. "maven_repository")))}
  :main ^:skip-aot gcpc.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
