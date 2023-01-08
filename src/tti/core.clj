(ns tti.core
  (:require [cljfx.api :as fx]
            [clojure.java.io :as io])
  (:import [java.awt.image BufferedImage]
           [javafx.embed.swing SwingFXUtils]
           [javax.imageio ImageIO]
           [javafx.scene SnapshotParameters]
           [javafx.scene.image WritableImage]))

(defn save-stage-as-png [node file-path]
  (let [buffered-image (SwingFXUtils/fromFXImage node (BufferedImage. 1920 1080 BufferedImage/TYPE_INT_ARGB))]
    (ImageIO/write buffered-image "png" (io/file file-path))))

(defn -main []
  (fx/on-fx-thread
   (let [stage (fx/create-component
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
                                            :text "Hi! What's your name?"}]}}})
         rt (.snapshot (.getRoot (.getScene (fx/instance stage))) (new SnapshotParameters) (new WritableImage 1920 1080))]
     (save-stage-as-png rt "stage4.png")
     (fx/delete-component stage))))