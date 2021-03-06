(comment "
Upload, process and store details on genomic variance.
")

(ns rvar.upload
  (:use [clojure.java.io]
        [clojure.contrib.str-utils]
        [ring.util.response :only [redirect]])
  (:require [rvar.model :as model]))

(defn parse-23andme [line-iter]
  "Lazily produce hash-map of variances from 23andMe text file."
  (let [line-info [:rsid :chr :start :genotype]]
    (for [line line-iter :when (not= \# (first line))]
      (zipmap line-info (re-split #"\t" line)))))

(defn variants-23andme [line-iter]
  "Convert raw 23andme data into standard variance objects."
  (for [raw (parse-23andme line-iter)]
    (let [start (Integer/parseInt (:start raw))]
      {:id (:rsid raw) :chr (:chr raw) :start start
       :end (+ 1 start) :genotype (:genotype raw)})))

(defn upload-23andme [request]
  "Upload a file of 23andme SNPs."
  (let [file-upload (get (request :multipart-params) "file")
        user (get (request :multipart-params) "user")
        fname (file-upload :filename)
        file-iter (-> file-upload :bytes reader line-seq)
        variants (variants-23andme file-iter)]
    (model/load-user-variants user fname variants))
  (redirect "/"))

(defn file-23andme [fname]
  "Lazy iterator of 23andme variances from a downloaded file."
  (let [vrn-lines (line-seq (reader fname))]
    (parse-23andme vrn-lines)))
