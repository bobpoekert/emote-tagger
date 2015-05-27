(ns emote-tagger.lucene
  (import [org.apache.lucene.index
            IndexWriter IndexWriterConfig IndexReader Term Terms
            AtomicReaderContext AtomicReader DirectoryReader MultiFields]
          [org.apache.lucene.util Version]
          [org.apache.lucene.store Directory MMapDirectory]
          org.apache.lucene.analysis.standard.StandardAnalyzer
          [org.apache.lucene.document Document Field]
          [org.apache.lucene.classification KNearestNeighborClassifier SimpleNaiveBayesClassifier])
  (require [clojure.java.io :as io]
           [clojure.data.json :as json])
  (use [emote-tagger.tokenize :only [tokenize-text]]))

(def lucene-version Version/LUCENE_40)

(defn directory
  ^Directory [^java.io.File d]
  (MMapDirectory. d)) 

(defn index-writer
  ([^IndexWriterConfig config ^java.io.File dir]
    (IndexWriter. (directory dir) config))
  ([^java.io.File dir]
    (index-writer
      (IndexWriterConfig.
        lucene-version
        (StandardAnalyzer. lucene-version))
      dir)))

(defprotocol LuceneDocument
  (to-document [this]))

(extend-protocol LuceneDocument
  Document
    (to-document [doc] doc)
  clojure.lang.ISeq 
    (to-document [docm]
      (let [^Document res (Document.)]
        (doseq [[k v] (seq docm)]
          (.add res
            (Field. (str k) (.getBytes (str v) "UTF-8"))))
        res))
  clojure.lang.Seqable
    (to-document [this] (to-document (seq this))))
              
(defn into-index
  [^IndexWriter index documents]
  (doseq [document documents]
    (.addDocument index (to-document document))))

(defn tagged-data-index
  [index-dir tagged-data]
  (let [index (index-writer (java.io.File. index-dir))]
    (with-open [inf (io/reader tagged-data)]  
      (into-index index
        (for [row (line-seq inf)]
          (let [[k v] (json/read-str row)]
            {"tag" k "content" v}))))
    index))

(defn atomic-readers
  [^IndexReader idx]
  (for [^AtomicReaderContext leaf (.leaves idx)]
    (.reader leaf)))

(defn get-terms
  [^AtomicReader rdr ^String class-field-name]
  (let [^Terms terms (MultiFields/getTerms rdr class-field-name)
        ^TermsEnum terms-enum (.iterator terms nil)]
    (iterator-seq terms-enum)))

(defn weak-memoize
  [thunk]
  (let [^java.util.Map cache (java.util.collections.Collections/synchromizedMap (java.util.WeakHashMap.))]
    (fn [& args]
      (if-let [res (.get cache args)]
        res
        (let [res (apply thunk args)]
          (.put cache args res)
          res)))))

(def n-docs
  (weak-memoize
    (fn [^AtomicReader rdr ^String class-field-name]
      (.getDocCount (MultiFields/getTerms rdr class-field-name)))))

(defn calculate-prior
  [^AtomicReader rdr ^BytesRef curernt-class]
  (let [

(defn class-probability
  [^AtomicReader rdr ^String class-field-name tokens]
  (for [^BytesRef term (get-terms rdr class-field-name)]
    

(defn bayes-clasifier
  [^IndexReader inp ^String text-field ^String class-field]
  (let [atr (atomic-readers inp)]
    (fn [^String target]
      (let [tokens (tokenize-text target)
            probabilities (for [^AtomicReaer rdr atr]
                            (future (class-probabilities rdr tokens)))]
        (merge-with * (map deref probabilities))))))

(defn knn-classifier
  ^KNearestNeighborClassifier
  ([^IndexWriter index]
    (knn-classifier index 20))
  ([^IndexWriter index k]
    (let [^KNearestNeighborClassifier res (KNearestNeighborClassifier. k)]
      (doseq [^AtomicReader rdr (atomic-readers (DirectoryReader/open index false))]
        (.train res
          rdr
          "tag" "content"
          (.getAnalyzer index)))
      res)))


(defn -main
  [& args]
  (with-open [index (tagged-data-index
                      "emotes.lucene"
                      "emotes.jsons")]
    index))
