(ns emote-tagger.svm
  (use [emote-tagger.tokenize :only [tokenize-text]]
       [clojure.set :only [map-invert]])
  (require [emote-tagger.liblinear :as linear]
           [clojure.data.json :as json]
           [clojure.java.io :as io])
  (import [de.bwaldvogel.liblinear Linear]))

(defn tagged-data
  [infname]
  (with-open [jsonf (io/reader infname)]
    (doall (map json/read-str (line-seq jsonf)))))

(defrecord Model
  [svm-model classes])

(defn top-n-tags
  [data n]
  (let [tag-counts (frequencies (map first data))
        top-k-tags (set (map first (take n
                                    (sort-by second #(compare %2 %1)
                                      (seq tag-counts)))))]
    (filter
      (fn [[k text]] (contains? top-k-tags k))
      data)))

(defn train
  [data]
  (let [bags-of-words (filter #(not (empty? (second %)))
                        (for [[tag text] data] [tag (set (tokenize-text text))]))
        classes (into {} (map-indexed
                          (fn [i e] [i e])
                          (set (map first bags-of-words))))
        reverse-classes (map-invert classes)
        class-indexes (for [[k v] bags-of-words] (get reverse-classes k))]
    (->Model
      (linear/train (map second bags-of-words) class-indexes :cross-fold 100)
      classes)))

(defn classify
  [model text]
  (get
    (.classes model)
    (linear/predict model (set (tokenize-text text)))))

(defn save-model
  [^Model model fname]
  (do
    (.mkdir (java.io.File. fname))
    (with-open [clsjs (io/writer (java.io.File. fname "classes.json"))]
      (json/write (.classes model) clsjs))
    (with-open [dimjs (io/writer (java.io.File. fname "dimensions.json"))]
      (json/write
        (:dimensions (.svm-model model))
        dimjs))
    (with-open [outf (io/writer (java.io.File. fname "model.bin"))]
      (Linear/saveModel outf (:liblinear-model (.svm-model model))))))

(defn load-model
  [fname]
  (with-open [modelfile (io/reader (java.io.File. fname "model.bin"))
              dimfile (io/reader (java.io.File. fname "dimensions.json"))
              classfile (io/reader (java.io.File. fname "classes.json"))]
    (->Model
      {
        :liblinear-model (Linear/loadModel modelfile)
        :dimensions (json/read dimfile)}
      (json/read classfile))))

(defn -main
  [& args]
  (save-model
    (train (top-n-tags (tagged-data "emotes.jsons") 10))
    "emotes.svm"))
