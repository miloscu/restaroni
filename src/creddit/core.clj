(ns creddit.core
  (:require [creddit.client :as client]))

(defprotocol RedditApi
  "Defines the Reddit API protocol."
  (listing [this names]
    "Retrieves a listing of Reddit items given their names.")
  (listing-comments [this subreddit article]
    "Retrieves the comments for a specific subreddit and article.")
  (more-child-comments [this linkId children]
    "Retrieves additional child comments for a given link ID and children IDs."))

;; A client for accessing the Reddit API using provided credentials.
(defrecord CredditClient [credentials]
  RedditApi
  (listing [_this names] (client/listing credentials names))
  (listing-comments [_this subreddit article] (client/listing-comments credentials subreddit article))
  (more-child-comments [_this linkId children] (client/more-child-comments credentials linkId children)))

(defn init
  "Initializes a CredditClient with the given credentials."
  [credentials]
  (let [response (client/get-access-token credentials)]
    (-> credentials
        (assoc :access-token (:access_token response))
        (assoc :expires-in (+ (System/currentTimeMillis) (:expires_in response)))
        (CredditClient.))))
