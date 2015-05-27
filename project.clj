(defproject emote-tagger "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main emote-tagger.lucene
  :jvm-opts ["-Xmx4096m"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clojure-opennlp "0.3.1"]
                 [clj-liblinear "0.1.0"]
                 [ring/ring-core "1.2.0"]
                 [org.clojure/data.json "0.2.3"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.apache.lucene/lucene-core "4.6.0"]
                 [org.apache.lucene/lucene-analyzers-common "4.6.0"]
                 [org.apache.lucene/lucene-classification "4.6.0"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [compojure "1.1.6"]])
