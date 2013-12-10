(ns emote-tagger.core
  (:gen-class)
  (use [compojure.core]
       [ring.adapter.jetty])
  (require [compojure.route :as route]
           [compojure.handler :as handler]
           [clojure.java.io :as io]
           [clojure.data.json :as json])
  (import [opennlp.tools.doccat DocumentCategorizerME DoccatModel]))

(deftype ClassifierResult
  [^DocumentCategorizerME model
    index-to-key key-to-index
    ^doubles values]
  
  clojure.lang.ILookup  
  (valAt [this k]
    (if (= k :best-category)
      (let [cat (.getBestCategory (.model this) (.values this))]
        [cat (aget (.values this) (get (.key-to-index this) cat))])
      (let [idx (get (.key-to-indx this) k)]
        (if (nil? idx)
          nil
          (aget (.values this) idx)))))
  
  (toString [this]
    (str (seq this)))
  
  clojure.lang.IKeywordLookup
  (getLookupThunk [result k]
    (reify clojure.lang.ILookupThunk
      (get [this target]
        (if (= :best-category k)
          (get result k)
          (get (str result) k)))))
  
  clojure.lang.Seqable
  (seq [this]
    (for [idx (range (alength (.values this)))]
      [(get (.index-to-key this) idx) (aget (.values this) idx)])))

(defn make-categorizer
  [fname]
  (let [^DocumentCategorizerME raw-categorizer
            (with-open [ins (io/input-stream fname)]
                (DocumentCategorizerME.
                  (DoccatModel. ins)))
        keynames (for [i (range (.getNumberOfCategories raw-categorizer))]
                  (.getCategory raw-categorizer i))
        idx-to-key (into {} (map (fn [k i] [i k])
                    keynames (range (count keynames))))
        key-to-idx (into {} (map (fn [k i] [k i])
                    keynames (range (count keynames))))]
    (fn [^String text]
      (ClassifierResult.
        raw-categorizer
        idx-to-key key-to-idx
        (.categorize raw-categorizer text)))))

(def -categorizer (atom nil))

(defn categorizer
  []
  (if @-categorizer
    @-categorizer
    (let [res (make-categorizer "emotes.bin")]
      (swap! -categorizer (fn [_] res))
      res)))

(defn json-response
  [res]
  {
    :status 200
    :headers {"Content-Type" "application/json"}
    :body (json/write-str res)})

(defn emote-tags
  [text]
  (let [categorizer (categorizer)]
    (reverse
      (sort-by second
        (filter
          (fn [[k v]] (>= v 0.01))
          (seq (categorizer text)))))))

(defroutes app-routes
  (GET "/emotes.json"
    {params :params}
    (do
      (println params)
      (json-response
        (emote-tags (:text params))))))

(defn -main
  [port & args]
  (run-jetty
    (handler/api app-routes)
    {:port (Integer/parseInt port)}))
