(ns tti.core
  (:require [cljfx.api :as fx]
            [clojure.java.io :as io]
            [username-generator.core :refer [generate-username]])
  (:import [java.awt.image BufferedImage]
           [javafx.embed.swing SwingFXUtils]
           [javax.imageio ImageIO]
           [javafx.scene SnapshotParameters]
           [javafx.scene.image WritableImage]))

(defn save-stage-as-png [node file-path]
  (let [buffered-image (SwingFXUtils/fromFXImage node (BufferedImage. 1920 1080 BufferedImage/TYPE_INT_ARGB))]
    (ImageIO/write buffered-image "png" (io/file file-path))))

(defn create-and-save-page [path comment]
  (try
    (println "Creating page: " (:name comment))
    (fx/on-fx-thread
     (let [ssp (new SnapshotParameters)
           stage (fx/create-component
                  {:fx/type :stage
                   :showing true
                   :always-on-top true
                   :style :transparent :width 1920 :height 1080
                   :scene {:fx/type :scene
                           :fill :transparent
                           :stylesheets #{"styles.css"}
                           :root {:fx/type :v-box
                                  :children [{:fx/type :label
                                              :wrap-text true
                                              :style {:-fx-font [32 :sans-serif]}
                                              :text (str (generate-username) (:ups comment))}
                                             {:fx/type :label
                                              :wrap-text true
                                              :style {:-fx-font [16 :sans-serif]}
                                              :text " "}
                                             {:fx/type :label
                                              :wrap-text true
                                              :style {:-fx-font [32 :sans-serif]}
                                              :text (str (:body comment))}]}}})
           rt (.snapshot (.getRoot (.getScene (fx/instance stage))) ssp (new WritableImage 1920 1080))]
       (save-stage-as-png rt path)
       (fx/delete-component stage)
       nil))
    (catch Exception e
      (println "Error creating page: " (:name comment)))))