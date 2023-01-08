(ns listing-transformer.core
  (:require [creddit.core :as creddit]
            [clojure.string :as string]))

(defn get-listing
  [client link]
  (creddit/listing client link))

(defn get-listing-comments
  [client subreddit link]
  (let [comments (creddit/listing-comments client subreddit link)]
    (hash-map :more-children (:children (:data (last (:children (:data (second comments))))))
              :comments (map #(select-keys (:data %) [:body, :ups]) (filter #(not= "[deleted]" %) (:children (:data (second comments))))))))

(defn get-listing-title-ups-awards-awardcount
  [client link]
  (let [listing (get-listing client link)
        comments (get-listing-comments client (:subreddit (first listing)) (second (string/split (first link) #"_")))]
    (merge (assoc (select-keys (first listing) [:title :ups])
                  :awards (sort-by :name (map #(select-keys % [:icon_url :count :name]) (:all_awardings (first listing)))))
           comments)))

(defn get-listing-morechildren 
  [client]
  ;; get :icon_url from each item in (:all_awardings (first (get-listing client link)))
  nil)
(defn get-listing-award-icon-urls
  [client link]
  (map #(select-keys % [:icon_url]) (:all_awardings (first (get-listing client link)))))