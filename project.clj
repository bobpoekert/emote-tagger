(defproject emote-tagger "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main emote-tagger.csv
  :jvm-opts ["-Xmx1024m"]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clojure-opennlp "0.3.1"]
                 [ring/ring-core "1.2.0"]
                 [org.clojure/data.json "0.2.3"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.apache.lucene/lucene-core "3.4.0"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [compojure "1.1.6"]])
