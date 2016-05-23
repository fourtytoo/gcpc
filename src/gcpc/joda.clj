(ns gcpc.joda
  (:require [clojure.instant :as i]
            [clj-time core format])
  (:import org.joda.time.DateTime))


(defmethod print-method org.joda.time.DateTime
  [^org.joda.time.DateTime d ^java.io.Writer w]
  (#'i/print-date (java.util.Date. (.getMillis d)) w))

(defmethod print-dup org.joda.time.DateTime
  [^org.joda.time.DateTime d ^java.io.Writer w]
  (#'i/print-date (java.util.Date. (.getMillis d)) w))

#_(defn construct-date-time [years months days hours minutes seconds nanoseconds
                           offset-sign offset-hours offset-minutes]
  (DateTime. (.getTimeInMillis (#'i/construct-calendar years months days
                                                       hours minutes seconds 0
                                                       offset-sign offset-hours offset-minutes))))

(defn construct-date-time [years months days hours minutes seconds nanoseconds
                           offset-sign offset-hours offset-minutes]
  (clj-time.core/date-time years months days
                           hours minutes seconds (/ nanoseconds 1000)))

#_(def read-instant-date-time
  "To read an instant as an org.joda.time.DateTime, bind *data-readers* to a
map with this var as the value for the 'inst key."
    (partial i/parse-timestamp (i/validated construct-date-time)))

(def read-instant-date-time
  "To read an instant as an org.joda.time.DateTime, bind *data-readers* to a
map with this var as the value for the 'inst key."
  (partial clj-time.core/date-time))

#_(defmacro with-joda-time-reader [& body]
  `(binding [*data-readers* (assoc *data-readers* '~'inst read-instant-date-time)]
     ~@body))

(defmacro with-joda-time-reader [& body]
  `(binding [*data-readers* (assoc *data-readers* '~'inst clj-time.format/parse)]
     ~@body))
