(ns html.core
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

(defn listing-page [final-map dirname]
  (html5
   [:head [:title "Restaroni"]]
   [:body {:style "width: 700px;"}
    [:h1 (:title final-map)]
    [:p (str "Upvotes: " (:ups final-map))]
    [:div
     [:a {:id "DL" :href (str "/resources/" dirname) :style "font-size:30pt;"} "Get images and audio"]]
    [:div "Awards:"]
    (map
     #(html5 [:div {:style "display: inline-block; margin: 10px; border-radius: 5px; border: 1px solid black; padding: 10px;"}
              [:img {:style "max-height: 25px; max-width: 25px;" :alt (:name %) :title (:name %) :src (:icon_url %)}]
              [:span (str " x " (:count %))]])
     (:awards final-map))
    (map
     #(html5
       [:div {:style "border-radius: 5px; border: 1px solid black; padding: 10px; margin: 10px;"}
        [:div (str "Comment ID " (:name %))]
        [:div (str "Upvotes " (:ups %))]
        [:div (:body %)]])
     (:comments final-map))]))

(defn resources-page [req dirname files]
  (html5
   [:head [:title "Restaroni"]]
   [:body
    [:h1 (:title req)]
    [:a {:id "DL" :href (str "/movies/" dirname)} "Generate silent movies (OLD)"]
    [:a {:id "DL" :href (str "/movies-new/" dirname)} "Generate silent movie (NEW)"]
    [:p (str dirname)]
    (map #(html5
           [:div
            [:img {:src (str "/" dirname "/" %) :width "384" :height "216"}]
            [:audio {:controls true}
             [:source {:src (str "/" dirname "/" (string/replace % ".png" ".wav")) :type "audio/wav"}]]])
         (reverse files))]))

(defn silent-movies-page [req dirname image-files]
  (html5
   [:head [:title "Restaroni"]]
   [:body
    [:h1 (:title req)]
    [:a {:id "DL" :href (str "/nonsilent/" dirname)} "Append audio"]

    (map #(html5
           [:div
            [:video {:controls true :height 216 :width 384}
             [:source {:src (str "/" dirname "/" (string/replace % ".png" (str "silent.mp4"))) :type "video/mp4"}]]])
         image-files)]))

(defn silent-movie-page [req dirname]
  (html5
   [:head [:title "Restaroni"]]
   [:body
    [:h1 (:title req)]
    [:a {:id "DL" :href (str "/finished-new/" dirname)} "Append audio"]

    (html5
     [:div
      [:video {:controls true :height 216 :width 384}
       [:source {:src (str "/" dirname "/ALL.mp4") :type "video/mp4"}]]])]))

(defn finished-movie-page [req dirname final-name]
  (html5
   [:head [:title "Restaroni"]]
   [:body
    [:h1 (:title req)]
    (html5
     [:div
      [:video {:controls true :height 216 :width 384}
       [:source {:src (str "/" dirname "/" final-name) :type "video/mp4"}]]])]))


(defn sound-movies-page [req dirname image-files]
  (html5
   [:head [:title "Restaroni"]]
   [:body
    [:h1 (:title req)]
    [:a {:id "DL" :href (str "/concatenated/" dirname)} "Concatenate videos (OLD)"]

    (map #(html5
           [:div
            [:video {:controls true :height 216 :width 384}
             [:source {:src (str "/" dirname "/" (string/replace % "silent.mp4" (str "sound.mp4"))) :type "video/mp4"}]]])
         image-files)]))

(defn voice-null-page []
  (html5
   [:head [:title "Restaroni"]]
   [:body
    [:p "Voice is null"]
    [:a {:href "/"} "Home"]]))