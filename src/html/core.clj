(ns html.core
  (:require [clojure.string :as string]
            [hiccup.form :refer [form-to text-field]]
            [hiccup.page :refer [html5]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn csrf-token []
  (anti-forgery-field))

(defn home-page "Home page" []
  (html5
   [:head [:title "Restaroni"]]
   [:body
    [:h1 "Restaroni"]
    (form-to [:post "/submit"]
             (csrf-token)
             [:label "App" (text-field {:name "app"} "App")]
             [:br]
             [:br]
             [:label "Secret" (text-field {:name "secret"} "Secret")]
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
    [:br]
    [:a {:id "DL" :href (str "/movies/" dirname)} "Generate silent movie (NEW)"]
    [:p (str dirname)]
    (map #(html5
           [:div
            [:img {:src (str "/" dirname "/" %) :width "384" :height "216"}]
            [:audio {:controls true}
             [:source {:src (str "/" dirname "/" (string/replace % ".png" ".wav")) :type "audio/wav"}]]])
         (reverse files))]))

(defn silent-movie-page [req dirname]
  (html5
   [:head [:title "Restaroni"]]
   [:body
    [:h1 (:title req)]
    [:a {:id "DL" :href (str "/finished/" dirname)} "Append audio"]

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
      [:video {:controls true :height 720 :width 1280}
       [:source {:src (str "/" dirname "/" final-name) :type "video/mp4"}]]])]))

(defn voice-null-page []
  (html5
   [:head [:title "Restaroni"]]
   [:body
    [:p "Voice is null"]
    [:a {:href "/"} "Home"]]))