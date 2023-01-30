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
                 [cljfx "1.7.22"]
                 [clj-commons/fs "1.6.310"]
                 [manifold "0.3.0"]
                 [net.sf.sociaal/freetts "1.2.2"]
                 [org.clojure/core.async "1.6.673"]
                 [org.bytedeco/ffmpeg "5.1.2-1.5.8"]
                 [org.bytedeco/ffmpeg-platform "5.1.2-1.5.8"]
                 [org.bytedeco/javacv "1.5.8"]
                 [org.bytedeco/javacv "1.5.8"]
                 [org.bytedeco/javacpp "1.5.8"]
                 [com.googlecode.mp4parser/isoparser "1.1.22"]]
  :repl-options {:init-ns restaroni.core}
  :jvm-opts ["-Xmx6G"]
  :main ^:skip-aot restaroni.core)