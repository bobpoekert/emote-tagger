(ns emote-tagger.tokenize
  (:import
   org.apache.lucene.analysis.standard.StandardAnalyzer
   org.apache.lucene.analysis.TokenStream
   org.apache.lucene.util.Version
   org.apache.lucene.analysis.tokenattributes.TermAttribute))

(defn tokenizer-seq
  "Build a lazy-seq out of a tokenizer with TermAttribute"
  [^TokenStream tokenizer ^TermAttribute term-att]
  (lazy-seq
    (when (.incrementToken tokenizer)
      (cons (.term term-att) (tokenizer-seq tokenizer term-att)))))

(defn load-analyzer ^StandardAnalyzer [^java.util.Set stopwords]
  (StandardAnalyzer. Version/LUCENE_CURRENT stopwords))

(def default-analyzer (load-analyzer StandardAnalyzer/STOP_WORDS_SET))

(defn tokenize-text
  "Apply a lucene tokenizer to cleaned text content as a lazy-seq"
  ([^StandardAnalyzer analyzer page-text]
    (let [reader (java.io.StringReader. page-text)
          tokenizer (.tokenStream analyzer nil reader)
          term-att (.addAttribute tokenizer TermAttribute)]
      (tokenizer-seq tokenizer term-att)))
  ([page-text] (tokenize-text default-analyzer page-text)))
