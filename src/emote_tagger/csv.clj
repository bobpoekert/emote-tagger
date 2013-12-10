(ns emote-tagger.csv
  (use [emote-tagger.tokenize :only [tokenize-text]])
  (require [clojure.java.io :as io]
           [clojure.data.json :as json]
           [clojure.data.csv :as csv]))

(defn tagged-rows
  [json-file]
  (map json/read-str (line-seq (io/reader json-file))))

(defn token-seq
  [json-file]
  (mapcat #(tokenize-text (second %)) (tagged-rows json-file)))

(defn all-words
  [fname]
  (with-open [json-file (io/reader fname)]
    (sort (into #{} (token-seq json-file)))))

(defn indexes
  [s]
  (persistent!
    (reduce
      (fn [out [idx v]]
        (assoc! out v idx))
      (transient {})
      (map-indexed (fn [e i] [e i]) s))))

(defn bags-of-words
  [inf]
  (let [words (all-words inf)
        word-count (count words)
        idxs (indexes words)]
    (cons
      (cons "tag" words)
      (for [[tag text] (tagged-rows inf)]
        (cons
          tag
          (reduce
            (fn [v [k cnt]]
              (let [idx (get idxs k)]
                (if idx
                  (assoc v (get idxs k) cnt)
                  v)))
            (apply vector-of :long (repeat word-count 0))
            (seq (frequencies (tokenize-text text)))))))))

(defn -main
  [infname outfname & args]
  (with-open [outf (io/writer outfname)]
    (csv/write-csv outf (bags-of-words infname))))

;(-main "emotes.jsons" "emotes.bow.csv")
