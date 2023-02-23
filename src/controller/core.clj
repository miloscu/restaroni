(ns controller.core
  (:require [clojure.string :as string]
            [creddit.core :as creddit]
            [hiccup.page :refer [html5]]
            [listing-transformer.core :as listing-transformer]
            [me.raynes.fs :as fs]
            [tts.core :as tts]
            [tti.core :as tti]
            [itv.core :as itv]
            [html.core :as html])
  (:import [com.sun.speech.freetts VoiceManager]))

(defn home-page []
  (html/home-page))

(defn get-listing-page [listing dirname req ct]
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
  ;; (itv/xreate (str fname ".mp4") (str fname ".png") (str fname ".au") 1920 1080)
    (html/resources-page req dirname files)))

(defn- get-silent-movies [dirname image-files req]
  (let [video-files (itv/find-files (str "./resources/public/" dirname) "silent.mp4")
        condition (= (count video-files) (count image-files))]
    (if condition
      (do
        (println "All movies created.")
        (html/silent-movies-page req dirname image-files))
      (do
        (println "Waiting for silent movies to be created.")
        (Thread/sleep 1000)
        (get-silent-movies dirname image-files req)))))

(defn- get-sound-movies [dirname image-files req]
  (let [video-files (itv/find-files (str "./resources/public/" dirname) "sound.mp4")
        condition (= (count video-files) (count image-files))]
    (if condition
      (do
        (println "All movies created.")
        (html/sound-movies-page req dirname image-files))
      (do
        (println "Waiting for sounded movies to be created.")
        (Thread/sleep 1000)
        (get-sound-movies dirname image-files req)))))

(defn movies-page [req]
  (let [dirname (:resource (:params req))
        image-files (sort (itv/find-files (str "./resources/public/" dirname) ".png"))
        mapped-files (map #(hash-map :image % :audio (string/replace % ".png" ".au")) image-files)
        __ (println mapped-files)]
    (doseq [img-audio-map (partition-all 5 (map #(hash-map :image (:image %) :audio (:audio %)) mapped-files))]
      (doseq [i img-audio-map]
        (itv/xreate
         (str "./resources/public/" dirname "/" (string/replace (:image i) ".png" "silent.mp4"))
         (str "./resources/public/" dirname "/" (:image i))
         (str "./resources/public/" dirname "/" (:audio i))
         1920
         1080)))

    (get-silent-movies dirname image-files req)))

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

(defn nonsilent-page [req]
  (let [dirname (:resource (:params req))
        movie-files (sort (itv/find-files (str "./resources/public/" dirname) "silent.mp4"))
        mapped-files (map #(hash-map :video % :audio (string/replace % "silent.mp4" ".au")) movie-files)
        __ (println mapped-files)]
    ;; (Thread/sleep (* 200 (count image-files)))

    (doseq [img-audio-map (partition-all 5 (map #(hash-map :video (:video %) :audio (:audio %)) mapped-files))]
      (doseq [i img-audio-map]
        (itv/merge-audio-and-video
         (str "./resources/public/" dirname "/" (:video i))
         (str "./resources/public/" dirname "/" (:audio i))
         (str "./resources/public/" dirname "/" (string/replace (:video i) "silent.mp4" "sound.mp4")))))

    (get-sound-movies dirname movie-files req)))

(defn concatenated-page [req]
  (let [dirname (:resource (:params req))
        movie-files (sort (itv/find-files (str "./resources/public/" dirname) "sound.mp4"))
        output (str "./resources/public/" dirname "/" dirname "_final.mp4")]
    (println (vec (map #(str "./resources/public/" dirname "/" %) movie-files)))
    (itv/conc (vec (map #(str "./resources/public/" dirname "/" %) movie-files)) output)
    (Thread/sleep 1000)
    (html5
     [:head [:title "Restaroni"]]
     [:body
      [:h1 (:title req)]
      [:a {:id "DL" :href (str "/" dirname "/" dirname "_final.mp4")} "Download final movie"]
      [:video {:controls true :height 216 :width 384} [:source {:src (str "/" dirname "/" dirname "_final.mp4") :type "video/mp4"}]]])))

;; VESTIGIAL CODE
;; t3_10vdmha1675719708880

;; (defn- filter-comments [creddit-client link-id upvotes kids]
;;   (filter (fn [z] (and (= (:parent_id z) link-id)
;;                        (contains? z :ups)
;;                        (> (:ups z) upvotes)))
;;           (flatten (map (fn [y] (map (fn [x] (select-keys x [:ups :body :parent_id :name]))
;;                                      (map :data (:things (:data (:json (creddit/more-child-comments creddit-client link-id (vec y))))))))
;;                         (partition 100 kids)))))

;; (defn- filter-comments [creddit-client link-id upvotes kids]
;;   (->> kids
;;        (partition 100)
;;        (pmap (fn [k] (creddit/more-child-comments creddit-client link-id (vec k))))
;;        (pmap (fn [response] (get-in response [:data :things])))
;;        (flatten)
;;        (pmap (fn [thing] (get-in thing [:data :json])))
;;        (pmap (fn [json] (select-keys json [:ups :body :parent_id])))
;;        (filterv (fn [comment] (println comment) (and (= (:parent_id comment) link-id)
;;                                    (contains? comment :ups)
;;                                    (> (:ups comment) upvotes))))))

;; (def rq (hash-map :params {:app "3Nb6bpRn7uTjgA"
;;                            :secret "48uwu2IIZp5t6CPxl3SzT1_6hEubww"
;;                            :thread "https://www.reddit.com/r/AskReddit/comments/zsl0mj/what_made_you_not_want_to_have_kids/"
;;                            :count "20"
;;                            :headers {"user-agent" "Restaroni/0.1.0"}}))