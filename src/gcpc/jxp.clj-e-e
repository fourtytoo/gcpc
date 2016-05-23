(ns gcpc.jxp
  (:import (javax.print DocFlavor$INPUT_STREAM)))

(defn print-request-attributes [& attrs]
  (javax.print.attribute.HashPrintRequestAttributeSet.))

(defn print-service-lookup [flavor attributes]
  (javax.print.PrintServiceLookup/lookupPrintServices flavor attributes))

(defn default-print-service-lookup []
  (javax.print.PrintServiceLookup/lookupDefaultPrintService))

(defn list-printers [&keys [flavor attributes]]
  (print-service-lookup DocFlavor$INPUT_STREAM/AUTOSENSE
                        (print-request-attributes)))

