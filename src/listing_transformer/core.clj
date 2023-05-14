(ns listing-transformer.core
  (:require [creddit.core :as creddit]
            [clojure.string :as string]))

(defn get-listing
  "Retrieves a listing from Reddit using the provided client and link.

  Args:
    client  - An instance of the Reddit client.
    link    - A string representing the link to the Reddit listing.

  Returns:
    The Reddit listing as returned by the 'listing' function."

  [client link]
  (creddit/listing client link))

(defn get-listing-comments
  "Retrieves comments from a Reddit listing using the provided client, subreddit, and link.

  Args:
    client     - An instance of the Reddit client.
    subreddit  - A string representing the subreddit.
    link       - A string representing the link to the Reddit listing.

  Returns:
    A map containing the retrieved comments:
    - :more-children - A vector of 'more' children in the comment tree.
    - :comments      - A sequence of maps containing the comment data, including 'body', 'ups', and 'name' fields.
                       Deleted comments are excluded from the result."

  [client subreddit link]
  (let [comments (creddit/listing-comments client subreddit link)]
    (hash-map :more-children (:children (:data (last (:children (:data (second comments))))))
              :comments (map #(select-keys (:data %) [:body, :ups, :name]) (filter #(not= "[deleted]" %) (:children (:data (second comments))))))))

(defn get-listing-title-ups-awards-awardcount
  "Retrieves the title, upvotes, awards, and award count of a Reddit listing, along with its comments.

  Args:
    client  - An instance of the Reddit client.
    link    - A string representing the link to the Reddit listing.

  Returns:
    A map containing the retrieved information:
    - :title       - The title of the listing.
    - :ups         - The number of upvotes the listing has received.
    - :awards      - A sequence of maps containing information about the awards, including 'icon_url', 'count', and 'name' fields.
    - :award-count - The total count of awards received by the listing.
    - :more-children - A vector of 'more' children in the comment tree.
    - :comments    - A sequence of maps containing the comment data, including 'body', 'ups', and 'name' fields.
                     Deleted comments are excluded from the result."

  [client link]
  (let [listing (get-listing client link)
        comments (get-listing-comments client (:subreddit (first listing)) (second (string/split (first link) #"_")))]
    (println listing)
    (merge (assoc (select-keys (first listing) [:title :ups])
                  :awards (sort-by :name (map #(select-keys % [:icon_url :count :name]) (:all_awardings (first listing)))))
           comments)))

(defn get-listing-morechildren
  [client]
  ;; get :icon_url from each item in (:all_awardings (first (get-listing client link)))
  nil)