(ns creddit.client
  (:require [clj-http.client :as client]
            [clojure.string :as string]
            [slingshot.slingshot :refer [try+]]))

(defn parse-response
  [response]
  (if-let [coll (or (get-in response [:data :children])
                    (get-in response [:data :trophies]))]
    (map :data coll)
    (:data response)))

(defn- valid-limit? [limit]
  (if (and (integer? limit)
           (<= 1 limit)
           (>= 100 limit))
    limit
    (throw
     (ex-info "Invalid limit - Must be an integer between 1 & 100."
              {:causes :invalid-limit}))))

(defn- valid-time? [time]
  (if (and (keyword? time)
           (contains? #{:hour :day :week :month :year :all} time))
    time
    (throw
     (ex-info "Invalid time - Must be one of the following: :hour, :day, :week, :month, :year, :all."
              {:causes :invalid-time}))))

(defn get-access-token-with-user
  [credentials]
  (try+
   (-> (client/post "https://www.reddit.com/api/v1/access_token"
                    {:basic-auth [(:user-client credentials) (:user-secret credentials)]
                     :headers {"User-Agent" "creddit"}
                     :form-params {:grant_type "password"
                                   :device_id (str (java.util.UUID/randomUUID))
                                   :username (:username credentials)
                                   :password (:password credentials)}
                     :content-type "application/x-www-form-urlencoded"
                     :socket-timeout 10000
                     :conn-timeout 10000
                     :as :json})
       (get :body))
   (catch [:status 401] {}
     (throw
      (ex-info "Unauthorised, please check your credentials are correct."
               {:causes :unauthorised})))))

(defn get-access-token-without-user
  [credentials]
  (try+
   (-> (client/post "https://www.reddit.com/api/v1/access_token"
                    {:basic-auth [(:user-client credentials) (:user-secret credentials)]
                     :headers {"User-Agent" "creddit"}
                     :form-params {:grant_type "client_credentials"
                                   :device_id (str (java.util.UUID/randomUUID))}
                     :content-type "application/x-www-form-urlencoded"
                     :socket-timeout 10000
                     :conn-timeout 10000
                     :as :json})
       (get :body))
   (catch [:status 401] {}
     (throw
      (ex-info "Unauthorised, please check your credentials are correct."
               {:causes :unauthorised})))))

(defn get-access-token
  [credentials]
  (if (:username credentials) (get-access-token-with-user credentials) (get-access-token-without-user credentials)))

(defn- http-get [credentials url]
  (-> (client/get url
                  {:basic-auth [(:access-token credentials)]
                   :headers {"User-Agent" "creddit"}
                   :socket-timeout 10000
                   :conn-timeout 10000
                   :as :json})
      (get :body)))

(defn listing
  [credentials names]
  (println (str "https://www.reddit.com/by_id/" (string/join "," names) "/.json"))
  (-> (http-get credentials (str "https://www.reddit.com/by_id/" (string/join "," names) "/.json"))
      (parse-response)))

(defn listing-comments
  [credentials subreddit article]
  (println  (str "https://www.reddit.com/r/" subreddit "/comments/" article ".json"))
  (http-get credentials (str "https://www.reddit.com/r/" subreddit "/comments/" article ".json")))

(defn more-child-comments
  [credentials linkId children]
  (http-get credentials (str "http://www.reddit.com/api/morechildren.json?link_id=" linkId "&children=" (string/join "," children) "&api_type=json")))
