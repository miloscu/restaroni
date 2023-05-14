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
   (GET "/"  [] (controller/home-page))
   (POST "/submit"  req (controller/submit-page req))
   (GET "/resources/:resource" req (controller/resources-page req))
   (GET "/movies-new/:resource" req (controller/movies-page-new req))
   (GET "/finished-new/:resource" req (controller/finished-page-new req))
   (route/resources "/")
   (route/not-found "Not Found")))

(def app
  (-> app-routes
      (content-type/wrap-content-type)
      (wrap-defaults site-defaults)))

(defn start-server []
  (jetty/run-jetty app {:port 8080}))

(defn -main
  [& _args]
  (start-server))