(defproject restaroni "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [compojure "1.7.0"]
                 [ring/ring-core "1.9.6"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [ring/ring-defaults "0.3.4"]
                 [ring/ring-json "0.5.0"]
                 [hiccup "1.0.5"]
                 [cheshire "5.11.0"]
                 [slingshot "0.12.2"]
                 [clj-http "3.12.3"]
                 [creddit "1.2.0"]
                 [org.openjfx/javafx-base "14" :classifier "win"]
                 [org.openjfx/javafx-graphics "14" :classifier "win"]
                 [org.openjfx/javafx-controls "14" :classifier "win"]
                 [org.openjfx/javafx-fxml "14" :classifier "win"]
                 [org.openjfx/javafx-swing "14" :classifier "win"]
                 [cljfx "1.7.22"]]
  :repl-options {:init-ns restaroni.core}
  :main ^:skip-aot restaroni.core)
