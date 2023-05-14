(ns controller.core
  (:require [clojure.string :as string]
            [creddit.core :as creddit]
            [listing-transformer.core :as listing-transformer]
            [me.raynes.fs :as fs]
            [tts.core :as tts]
            [tti.core :as tti]
            [itv.core :as itv]
            [html.core :as html])
  (:import [com.sun.speech.freetts VoiceManager]))

(defn home-page []
  (html/home-page))

(defn get-listing-page
  [listing dirname req ct]
  (let [image-files (itv/find-files (str "./resources/public/" dirname) "png")]
    (if (or (= (count image-files) (inc ct)) (= (count image-files) (inc (count (:comments listing))))) ;; +1 for title image
      (do
        (println "All images created.")
        (html/listing-page listing dirname))
      (do
        (println "Waiting for images to be created.")
        (Thread/sleep 1000)
        (get-listing-page listing dirname req ct)))))

(defn submit-page [request]
  (let [params (:params request)
        thread (:thread params)
        user-client (:app params)
        user-secret (:secret params)
        ct (parse-long (:count params))
        creddit-client (creddit/init {:user-client user-client :user-secret user-secret})
        link-id (str "t3_" (nth (string/split thread #"\/") 6))
        listing (listing-transformer/get-listing-title-ups-awards-awardcount creddit-client [link-id])
        ;; kids (:more-children listing)
        ;; more-children-comments (filter-comments creddit-client link-id 10 kids)
        _ (println listing)
        final-map (hash-map :title (:title listing)
                            :ups (:ups listing)
                            :awards (:awards listing)
                            :comments (take ct (reverse (sort-by :ups (concat (:comments listing))))))
        dirname (str link-id "_" (System/currentTimeMillis))]
    (println "params: " (dissoc params :__anti-forgery-token))
    (println "link-id: " link-id)

    ;; Create folder with link-id name
    (if (fs/exists? (str "./resources/public/" dirname))
      (println "Directory already exists.")
      (do (fs/mkdir (str "./resources/public/" dirname))
          (println "Directory created.")))

    (let [_ (System/setProperty "freetts.voices" "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory")
          voice (.getVoice (VoiceManager/getInstance) "kevin16")]
      (if (nil? voice)
        (do
          (println "Voice is null")
          (html/voice-null-page))

        (do
          (.allocate voice)
          (.setRate voice 190)
          (.setPitch voice 150)
          (.setVolume voice 3)

          (let [com (filter #(contains? % :body) (:comments final-map))]
            ;; title screen and audio
            (let [fname (str "./resources/public/" dirname "/_____title")]
              (tts/speakaroni fname (:title final-map) final-map voice)
              (tti/create-and-save-page (str fname ".png") {:name (:title final-map) :ups (:ups final-map) :awards (:awards final-map) :body (:title final-map)}))
            ;; comments to images and audio
            (doseq [comments (partition-all 5 (map #(hash-map :name (:name %) :body (:body %) :ups (:ups %)) com))]
              (doseq [i comments]
                (let [fname (str "./resources/public/" dirname "/" (:name i))]
                  (tts/speakaroni fname (:body i) i voice)
                  (tti/create-and-save-page (str fname ".png") i))))
            (Thread/sleep (* 200 (count com)))
            (println listing)
            (println (:awards final-map))
            (get-listing-page final-map dirname request ct)))))))

(defn resources-page [req]
  (let [dirname (:resource (:params req))
        files (itv/find-files (str "./resources/public/" dirname) "png")]
    (html/resources-page req dirname files)))

(defn get-silent-movie [dirname req]
  (let [video-file (itv/find-files (str "./resources/public/" dirname) "ALL.mp4")
        condition (= (count video-file) 1)]
    (if condition
      (do
        (println "Movie created.")
        (html/silent-movie-page req dirname))
      (do
        (println "Waiting for silent movie to be created.")
        (Thread/sleep 1000)
        (get-silent-movie dirname req)))))

(defn movies-page-new [req]
  (let [dirname (:resource (:params req))]

    (itv/folder-to-movies2 (str "./resources/public/" dirname))
    (get-silent-movie dirname req)))

(defn- get-finished-movie [dirname req final-name]
  (let [video-file (itv/find-files (str "./resources/public/" dirname) "ALL.mp4")
        condition (= (count video-file) 1)]
    (if condition
      (do
        (println "Movie created.")
        (html/finished-movie-page req dirname final-name))
      (do
        (println "Waiting for sounded movie to be created.")
        (Thread/sleep 1000)
        (get-finished-movie dirname req final-name)))))

(defn finished-page-new [req]
  (println "finished-page-new")
  (let [dirname (:resource (:params req))
        audio-files (sort (itv/find-files (str "./resources/public/" dirname) ".au"))
        final-name (str "FINISHED" (System/currentTimeMillis) ".mp4")]
    ;; (Thread/sleep (* 200 (count image-files)))
    (itv/merge-audios-and-video
     (str "./resources/public/" dirname "/ALL.mp4")
     (map #(str "./resources/public/" dirname "/" %) audio-files)
     (str "./resources/public/" dirname "/" final-name))
    (get-finished-movie dirname req final-name)))