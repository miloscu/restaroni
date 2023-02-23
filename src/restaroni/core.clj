(ns restaroni.core
  (:require [clojure.java.io :as io]
            [compojure.core :refer [GET POST routes]]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.content-type :as content-type]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [controller.core :as controller]))

(def app-routes
  (routes
   (GET "/favicon.ico" [] (slurp (io/resource "favicon.ico")))
   (GET "/" [] 
     (controller/home-page))
   (POST "/submit" request
     (controller/submit-page request))
   (GET "/resources/:resource" req
     (controller/resources-page req))
   (GET "/movies/:resource" req
     (controller/movies-page req))
   (GET "/movies-new/:resource" req
     (controller/movies-page-new req))
   (GET "/finished-new/:resource" req
     (controller/finished-page-new req))
   (GET "/nonsilent/:resource" req
     (controller/nonsilent-page req))
   (GET "/concatenated/:resource" req
     (controller/concatenated-page req))
   (route/resources "/")
   (route/not-found "Not Found")))

(def app
  (-> app-routes
      (content-type/wrap-content-type)
      (wrap-defaults site-defaults)))

(defn start-server []
  (jetty/run-jetty app {:port 8080}))

(defn -main
  "I don't do a whole lot...yet."
  [& args]
  (start-server))

;; (def app
;;   (-> app-routes
;;       (wrap-defaults site-defaults)))

;; (def creds {:user-client "3Nb6bpRn7uTjgA",
;;             :user-secret "48uwu2IIZp5t6CPxl3SzT1_6hEubww"})

;; (def creddit-client (creddit/init creds))

;; (def my-listing
;;   (listing-transformer/get-listing-title-ups-awards-awardcount creddit-client ["t3_zsl0mj"]))

;; (def kids (:more-children my-listing))

;; (def more-children-comments-with-over-10-upvotes
;;   (filter #(and (= (:parent_id %) "t3_zsl0mj") (contains? % :ups) (> (:ups %) 10)) (flatten (map #(map (fn [x] (select-keys x [:ups :body :parent_id])) (map :data (:things (:data (:json (creddit/more-child-comments creddit-client "t3_zsl0mj" (vec %))))))) (partition 100 kids)))))

;; (def final-map
;;   (hash-map :title (:title my-listing)
;;             :ups (:ups my-listing)
;;             :awards (:awards my-listing)
;;             :comments (sort-by :ups (concat (:comments my-listing) more-children-comments-with-over-10-upvotes))))

;; (count (:comments final-map))

;; (defn- final-map-html5 []
;;   (html5
;;    [:head [:title "Submission Received"]]
;;    [:body
;;     [:h1 (:title final-map)]
;;     [:p (str "Upvotes: " (:ups final-map))]
;;     (map #(html5 [:p (str "Awards: " (:name %) " " (:count %))]) (:awards final-map))
;;     (map #(html5 [:p "------------"] [:p (str (:ups %) "\t " (:body %))]) (:comments final-map))]))




;; (println (creddit-client :get "/r/clojure"))
;; (creddit/listing creddit-client ["t3_zsl0mj"])
;; (select-keys (first (creddit/listing creddit-client ["t3_zsl0mj"])) [:title :ups])
;; (map #(select-keys % [:icon_url :count]) (:all_awardings (first (creddit/listing creddit-client ["t3_zsl0mj"]))))

;; (flatten (map #(map (fn [x] (select-keys x [:ups :body])) (map :data (:things (:data (:json (creddit/more-child-comments creddit-client "t3_zsl0mj" (vec %))))))) (take 2 (partition 100 (:more-children (listing-transformer/get-listing-title-ups-awards-awardcount creddit-client ["t3_zsl0mj"]))))))

;; (filter #(and (contains? % :ups) (> (:ups %) 10)) (flatten (map #(map (fn [x] (select-keys x [:ups :body])) (map :data (:things (:data (:json (creddit/more-child-comments creddit-client "t3_zsl0mj" (vec %))))))) (partition 100 (:more-children (listing-transformer/get-listing-title-ups-awards-awardcount creddit-client ["t3_zsl0mj"]))))))
;; (fn [y] (and (contains? (keys y) :ups) (> (:ups y) 10)))
;; filter (fn [y] (and (contains? (keys y) :ups) (> (:ups y) 10)))
;; (partition 100 (:more-children (listing-transformer/get-listing-title-ups-awards-awardcount creddit-client ["t3_zsl0mj"])))

;; (listing-transformer/get-listing-title-ups-awards-awardcount creddit-client ["t3_zsl0mj"])

;; (listing-transformer/get-listing-comments creddit-client "AskReddit" "zsl0mj")
;; (map #(select-keys % [:icon_url :count]) (:all_awardings (listing-transformer/get-listing-title-ups-awards-awardcount creddit-client ["t3_zsl0mj"])))
;; (creddit/new creddit-client 10 :hour)
;; (def comments (creddit/listing-comments creddit-client "AskReddit" "zsl0mj"))
;; (creddit/listing-comments creddit-client "AskReddit" "zsl0mj")
;; (first (creddit/listing-comments creddit-client "AskReddit" "zsl0mj"))
;; (creddit/more-child-comments creddit-client ["zsl0mj"] ["j18egqp"])
;; (creddit/api-raw creddit-client "http://www.reddit.com/api/morechildren.json?link_id=t3_zsl0mj&children=j18egqp&api_type=json")

;; (creddit/more-child-comments creddit-client "t3_zsl0mj" ["j19zu2t" "j196c5j" "j1bjbii" "j1a1laa" "j1bgidr" "j18v3m8" "j19ajv6"])

;; (defn get-morechildren-with-more-than-x-upvotes
;;   "Filter comments with more than 10 upvotes"
;;   [client post-id ups]
;;   (let [comments (:more-children (listing-transformer/get-listing-title-ups-awards-awardcount client [post-id]))
;;         filtered-comments (for [comment comments]
;;                             (when (and (contains? comment :ups)
;;                                        (> (:ups comment) ups))
;;                               (select-keys comment [:ups :body])))
;;         nested-comments (for [chunk (partition 100 comments)
;;                               data (:things (:data (:json (creddit/more-child-comments client post-id (vec chunk)))))]
;;                           (map #(select-keys % [:ups :body]) (:data data)))]
;;     (flatten (concat filtered-comments nested-comments))))