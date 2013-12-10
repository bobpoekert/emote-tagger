(ns emote-tagger.train
  (use [emote-tagger.tokenize :only [tokenize-text]])
  (require [clojure.java.io :as io]
           [clojure.data.json :as json]))

(defrecord BayesModel
  [tags
  n-words
  word-tag-counts
  tag-counts
  word-counts])

(defn bayes-counts
  [tagged-data]
  (reduce
    (fn [model [tag text]]
      (let [words (tokenize-text text)]
        (->BayesModel
          (conj (.tags model) tag) ; tags

          (+ (.n-words model) (count words)) ; n-words

          (merge-with +
            (.word-tag-counts model) ; word-tag-counts
            (frequencies (for [word words] [tag word])))

          (merge-with + (.tag-counts model) {tag 1}) ; tag-counts

          (merge-with +
            (.word-counts model)
            (frequencies words))))) ; word-counts
    (->BayesModel #{} 0 {} {} {})
    tagged-data))

'(bayes-counts [["foo" "this is something"] ["bar" "this is something else"]])

(defn train
  [fname]
  (with-open [inr (io/reader fname)]
    (let [rows (map json/read-str (line-seq inr))]
      (bayes-counts rows))))

(defn tag-prob
  [model word tag]
    (let [word-given-tag (get (.word-tag-counts model) [tag word] 0)
          tag (get (.tag-counts model) tag 0)
          word (get (.word-counts model) word 0)
          all-tags (count (.tags model))
          all-words (.n-words model)
          p-word-given-tag (/ word-given-tag tag)
          p-word (/ word all-words)
          p-tag (/ tag all-tags)]
      (/ (* p-word-given-tag p-word) p-tag)))

(defn classify
  [model text]
  (let [token-counts (frequencies (tokenize-text text))
        token-probs (for [tag (.tags model)]
                      [
                        tag
                        (reduce *
                          (for [[token total] (seq token-counts)]
                            (Math/pow (tag-prob model token tag) total)))])]
    (sort-by second #(compare %2 %1) token-probs)))

(defn save-obj
  [obj outf]
  (with-open [outf (java.io.ObjectOutputStream. (io/output-stream outf))]
    (.writeObject outf obj)))

(defn load-obj
  [inf]
  (with-open [inf (java.io.ObjectInputStream. (io/input-stream inf))]
    (.readObject inf)))

(defn -main
  [inf outf & args]
  (save-obj (train inf) outf))
