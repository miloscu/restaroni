(ns creddit.core
  (:require [creddit.client :as client]))

(defprotocol RedditApi
  (listing [this names])
  (listing-comments [this subreddit article])
  (more-child-comments [this linkId children]))

(defrecord CredditClient [credentials]
  RedditApi
  (listing [this names] (client/listing credentials names))
  (listing-comments [this subreddit article] (client/listing-comments credentials subreddit article))
  (more-child-comments [this linkId children] (client/more-child-comments credentials linkId children)))

(defn init
  [credentials]
  (let [response (client/get-access-token credentials)]
    (-> credentials
        (assoc :access-token (:access_token response))
        (assoc :expires-in (+ (System/currentTimeMillis) (:expires_in response)))
        (CredditClient.))))
