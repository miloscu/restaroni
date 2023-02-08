(ns controller.core
  (:require [clojure.core.reducers :as reducers]
            [clojure.string :as string]
            [creddit.core :as creddit]
            [hiccup.form :refer [form-to text-field]]
            [hiccup.page :refer [html5]]
            [listing-transformer.core :as listing-transformer]
            [me.raynes.fs :as fs]
            [tts.core :as tts]
            [tti.core :as tti]
            [itv.core :as itv]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [clojure.string :as str]
            [cheshire.core :as json])
  (:import [com.sun.speech.freetts VoiceManager]))

(defn csrf-token []
  (anti-forgery-field))

(defn home-page []
  (html5
   [:head [:title "Restaroni"]]
   [:body
    [:h1 "Restaroni"]
    (form-to [:post "/submit"]
             (csrf-token)
             [:label "App" (text-field {:name "app" :value "3Nb6bpRn7uTjgA"} "App")]
             [:br]
             [:br]
             [:label "Secret" (text-field {:name "secret" :value "48uwu2IIZp5t6CPxl3SzT1_6hEubww"} "Secret")]
             [:br]
             [:br]
             [:label "Thread" (text-field {:name "thread"} "Thread 'http.../'")]
             [:br]
             [:br]
             [:label "Count" (text-field {:name "count"} "Count")]
             [:br]
             [:br]
             [:input {:type "submit" :value "Submit"}])]))


(defn- filter-comments [creddit-client link-id upvotes kids]
  (filter (fn [z] (and (= (:parent_id z) link-id)
                       (contains? z :ups)
                       (> (:ups z) upvotes)))
          (flatten (map (fn [y] (map (fn [x] (select-keys x [:ups :body :parent_id :name]))
                                     (map :data (:things (:data (:json (creddit/more-child-comments creddit-client link-id (vec y))))))))
                        (partition 100 kids)))))

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

(def rq (hash-map :params {:app "3Nb6bpRn7uTjgA"
                           :secret "48uwu2IIZp5t6CPxl3SzT1_6hEubww"
                           :thread "https://www.reddit.com/r/AskReddit/comments/zsl0mj/what_made_you_not_want_to_have_kids/"
                           :count "20"
                           :headers {"user-agent" "Restaroni/0.1.0"}}))

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
        final-map (hash-map :title (:title listing)
                            :ups (:ups listing)
                            :awards (:awards listing)
                            :comments (sort-by :ups (concat (:comments listing))))
        dirname (str link-id (System/currentTimeMillis))]
    (println "params: " (dissoc params :__anti-forgery-token))
    (println "link-id: " link-id)

    ;; Create folder with link-id name
    (if (fs/exists? (str "./resources/public/" dirname))
      (println "Directory already exists.")
      (do (fs/mkdir (str "./resources/public/" dirname))
          ;; (fs/mkdir (str link-id "/img"))
          ;; (fs/mkdir (str link-id "/wav"))
          (println "Directory created.")))

    (let [_ (System/setProperty "freetts.voices" "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory")
          voice (.getVoice (VoiceManager/getInstance) "kevin16")]
      (if (nil? voice)
        (do
          (println "Voice is null")
          (html5
           [:head [:title "Restaroni"]]
           [:body
            [:p "Voice is null"]
            [:a {:href "/"} "Home"]]))

        (do
          (.allocate voice)
          (.setRate voice 190)
          (.setPitch voice 150)
          (.setVolume voice 3)

          (let [com (filter #(contains? % :body) (:comments final-map))]
        ;; (map #(:body %) com)
            (doseq [comments (partition-all 5 (map #(hash-map :name (:name %) :body (:body %)) com))]
              (doseq [i comments]
                (let [fname (str "./resources/public/" dirname "/" (:name i))]
                  (tts/speakaroni fname (:body i) i voice)
                  (tti/create-and-save-page (str fname ".png") i))))
            (Thread/sleep (* 200 (count com)))
            (html5
             [:head [:title "Restaroni"]]
             [:body
              [:h1 (:title final-map)]
              [:div
               [:a {:href "../file.mp4"} "Link"]
               [:span " "]
               [:a {:id "DL" :href (str "/resources/" dirname)} "Link"]]
              [:p (str "Upvotes: " (:ups final-map))]
              (map #(html5 [:p (str "Awards: " (:name %) " " (:count %))]) (:awards final-map))
              (map #(html5 [:p "------------"] [:p (str (:name %) (:ups %) "\t " (:body %))]) (:comments final-map))])))))))

(defn resources-page [req]
  (let [dirname (:resource (:params req))
        files (itv/find-files (str "./resources/public/" dirname) "png")]
  ;; (itv/xreate (str fname ".mp4") (str fname ".png") (str fname ".au") 1920 1080)
    (html5
     [:head [:title "Restaroni"]]
     [:body
      [:h1 (:title req)]
      [:a {:id "DL" :href (str "/movies/" dirname)} "Generate movies"]
      [:p (str dirname)]
      (map #(html5
             [:div
              [:img {:src (str "/" dirname "/" %) :width "384" :height "216"}]
              [:audio {:controls true} [:source {:src (str "/" dirname "/" (string/replace % ".png" ".wav")) :type "audio/wav"}]]]) files)
              ;; [:div
              ;;  [:a {:download true :href (str "/resources" dirname)} "Link"]]
              ;; [:p (str "Upvotes: " (:ups final-map))]
              ;; (map #(html5 [:p (str "Awards: " (:name %) " " (:count %))]) (:awards final-map))
              ;; (map #(html5 [:p "------------"] [:p (str (:name %) (:ups %) "\t " (:body %))]) (:comments final-map))
      ])))

(defn- get-silent-movies [dirname image-files req]
  (let [video-files (itv/find-files (str "./resources/public/" dirname) "silent.mp4")
        condition (= (count video-files) (count image-files))]
    (if condition
      (do
        (println "All movies created.")
        (html5
         [:head [:title "Restaroni"]]
         [:body
          [:h1 (:title req)]
          [:a {:id "DL" :href (str "/nonsilent/" dirname)} "Append audio"]

          (map #(html5
                 [:div
                  [:video {:controls true :height 216 :width 384} [:source {:src (str "/" dirname "/" (string/replace % ".png" (str "silent.mp4"))) :type "video/mp4"}]]]) image-files)]))
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
        (html5
         [:head [:title "Restaroni"]]
         [:body
          [:h1 (:title req)]
          [:a {:id "DL" :href (str "/concatenated/" dirname)} "Concatenate videos"]

          (map #(html5
                 [:div
                  [:video {:controls true :height 216 :width 384} [:source {:src (str "/" dirname "/" (string/replace % "silent.mp4" (str "sound.mp4"))) :type "video/mp4"}]]]) image-files)]))
      (do
        (println "Waiting for sounded movies to be created.")
        (Thread/sleep 1000)
        (get-sound-movies dirname image-files req)))))

(defn movies-page [req]
  (let [dirname (:resource (:params req))
        image-files (sort (itv/find-files (str "./resources/public/" dirname) ".png"))
        mapped-files (map #(hash-map :image % :audio (string/replace % ".png" ".au")) image-files)
        __ (println mapped-files)]
    ;; (Thread/sleep (* 200 (count image-files)))

    (doseq [img-audio-map (partition-all 5 (map #(hash-map :image (:image %) :audio (:audio %)) mapped-files))]
      (doseq [i img-audio-map]
        (itv/xreate
         (str "./resources/public/" dirname "/" (string/replace (:image i) ".png" "silent.mp4"))
         (str "./resources/public/" dirname "/" (:image i))
         (str "./resources/public/" dirname "/" (:audio i))
         1920
         1080)))

    (get-silent-movies dirname image-files req)))

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
;; t3_10vdmha1675719708880