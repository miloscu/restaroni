(ns controller.core
  (:require [clojure.string :as string]
            [creddit.core :as creddit]
            [listing-transformer.core :as listing-transformer]
            [me.raynes.fs :as fs]
            [tts.core :as tts]
            [tti.core :as tti]
            [itv.core :as itv]
            [html.core :as html])
  (:import [com.sun.speech.freetts VoiceManager]))

(defn home-page []
  (html/home-page))

(defn get-listing-page
  "Returns an HTML page with the listing information and images of the items.

   Args:
     listing - Map containing the information of the items to be displayed on the page.
     dirname - String representing the directory where the item images are stored.
     req     - Request object passed from the Ring handler.
     ct      - Integer representing the number of comments in the listing.

   This function uses the `find-files` function from the `itv` namespace to get a list of image files
   in the specified directory. It then checks if the number of images found matches the expected
   number of images for the listing. If the number of images is correct, the function returns an HTML
   page with the listing information and images. If the number of images is not correct, the function
   waits for one second and then calls itself recursively to check again. This process continues until
   the correct number of images is found.

   Side effects:
   - Calls the `find-files` function from the `itv` namespace.
   - May sleep the thread for one second.
   - Prints messages to the console indicating the status of the image creation process.
   
   Returns:
     An HTML page with the listing information and images."

  [listing dirname req ct]
  (let [image-files (itv/find-files (str "./resources/public/" dirname) "png")]
    (if (or (= (count image-files) (inc ct)) (= (count image-files) (inc (count (:comments listing))))) ;; +1 for title image
      (do
        (println "All images created.")
        (html/listing-page listing dirname))
      (do
        (println "Waiting for images to be created.")
        (Thread/sleep 1000)
        (get-listing-page listing dirname req ct)))))

(defn submit-page
  "Generates a submission page with title, comments, and images based on the given request.

   Args:
     request - a request object passed from the Ring handler.

   This function processes the `params` from the request to extract relevant information such as the
   thread, user client, user secret, and comment count. It then initializes the Creddit client using
   the user client and user secret. Next, it extracts the `link-id` from the thread URL and retrieves
   the listing information for that link using the Creddit client. The retrieved listing information
   includes the title, upvotes, awards, and comments.

   The function creates a new directory with a unique name based on the `link-id` and the current
   timestamp. It checks if the directory already exists and prints the corresponding message. If the
   directory doesn't exist, it creates the directory and prints a message indicating the successful
   creation.

   Next, the function sets up the text-to-speech voice using the FreeTTS library. It checks if the
   voice is null and, if so, returns a page indicating that the voice is null. If the voice is not
   null, it proceeds to allocate the voice and set its rate, pitch, and volume.

   The function then processes the comments from the listing to filter out comments without a body.
   It generates a title screen and audio by converting the title text to speech and saving an image
   of the title. It iterates over the comments in groups of five, converting each comment body to
   speech and saving an image of the comment. The function also sleeps for a duration based on the
   count of comments to allow time for the audio and image generation.

   Finally, the function calls the `get-listing-page` function with the final map of listing
   information, the directory name, the request, and the comment count. This function retrieves the
   image files for the listing, waits for all images to be created, and returns an HTML page with
   the listing information and images.

   Side effects:
   - Prints various messages to the console indicating the status of directory creation, voice
     allocation, and image creation.
   - Uses the FreeTTS library to generate text-to-speech audio files.
   - Uses the TTIGenerator namespace to generate and save PNG images.
   - Calls the `get-listing-page` function.

   Returns: an HTML page with the listing information and images."

  [request]
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
        _ (println listing)
        final-map (hash-map :title (:title listing)
                            :ups (:ups listing)
                            :awards (:awards listing)
                            :comments (take ct (reverse (sort-by :ups (concat (:comments listing))))))
        dirname (str link-id "_" (System/currentTimeMillis))]
    (println "params: " (dissoc params :__anti-forgery-token))
    (println "link-id: " link-id)

    ;; Create folder with link-id name
    (if (fs/exists? (str "./resources/public/" dirname))
      (println "Directory already exists.")
      (do (fs/mkdir (str "./resources/public/" dirname))
          (println "Directory created.")))

    (let [_ (System/setProperty "freetts.voices" "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory")
          voice (.getVoice (VoiceManager/getInstance) "kevin16")]
      (if (nil? voice)
        (do
          (println "Voice is null")
          (html/voice-null-page))

        (do
          (.allocate voice)
          (.setRate voice 190)
          (.setPitch voice 150)
          (.setVolume voice 3)

          (let [com (filter #(contains? % :body) (:comments final-map))]
            ;; title screen and audio
            (let [fname (str "./resources/public/" dirname "/_____title")]
              (tts/text-to-speech fname (:title final-map) final-map voice)
              (tti/create-and-save-page (str fname ".png") {:name (:title final-map) :ups (:ups final-map) :awards (:awards final-map) :body (:title final-map)}))
            ;; comments to images and audio
            (doseq [comments (partition-all 5 (map #(hash-map :name (:name %) :body (:body %) :ups (:ups %)) com))]
              (doseq [i comments]
                (let [fname (str "./resources/public/" dirname "/" (:name i))]
                  (tts/text-to-speech fname (:body i) i voice)
                  (tti/create-and-save-page (str fname ".png") i))))
            (Thread/sleep (* 200 (count com)))
            (println listing)
            (println (:awards final-map))
            (get-listing-page final-map dirname request ct)))))))

(defn resources-page
  "Generates a resources page for the specified directory.

   Args:
     req - Request object passed from the Ring handler.

   This function retrieves the `dirname` parameter from the request's `params` and uses it to find
   PNG files in the corresponding directory within the './resources/public/' path. The function
   then calls the `html/resources-page` function, passing in the request, dirname, and the list of
   found files.

   Returns:
     An HTML resources page displaying the files in the specified directory."

  [req]
  (let [dirname (:resource (:params req))
        files (itv/find-files (str "./resources/public/" dirname) "png")]
    (html/resources-page req dirname files)))

(defn get-silent-movie
  "Generates a silent movie page for the specified directory.
   
   Args:
     dirname - the name of the directory containing the movie file.
     req - a request object passed from the Ring handler.

   This function searches for a single video file with the extension 'ALL.mp4' in the specified
   directory within the './resources/public/' path. If such a file is found, the function calls
   the `html/silent-movie-page` function, passing in the request and dirname, and returns the HTML
   silent movie page.

   If no matching file is found, the function waits for a short period and recursively calls itself
   until the movie file is created.

   Returns: an HTML silent movie page if the movie file exists, otherwise waits and calls itself
   recursively."
  [dirname req]
  (let [video-file (itv/find-files (str "./resources/public/" dirname) "ALL.mp4")
        condition (= (count video-file) 1)]
    (if condition
      (do
        (println "Movie created.")
        (html/silent-movie-page req dirname))
      (do
        (println "Waiting for silent movie to be created.")
        (Thread/sleep 1000)
        (get-silent-movie dirname req)))))

(defn movies-page-new
  "Generates a movies page for the specified directory.
   
   Args:
     req - a request object passed from the Ring handler.

   This function retrieves the `dirname` from the `:params` field of the request object. It then
   calls the `itv/folder-to-movies2` function to convert the images in the specified directory into
   a silent movie. Once the silent movie is created, it calls the `get-silent-movie` function to
   display the movies page.

   The `dirname` parameter specifies the name of the directory containing the images for the movie.
   
   Returns: an HTML movies page displaying the silent movie for the specified directory."

  [req]
  (let [dirname (:resource (:params req))]

    (itv/folder-to-movies2 (str "./resources/public/" dirname))
    (get-silent-movie dirname req)))

(defn- get-finished-movie
  "Gets the finished movie for the specified directory.
   
   Args:
     dirname - the name of the directory containing the movie files.
     req - a request object passed from the Ring handler.
     final-name - the name of the final movie file.

   This function checks if the finished movie file exists in the specified directory. If the file
   is found, it calls the `html/finished-movie-page` function to display the finished movie page.
   If the file is not found, it waits for the sounded movie to be created before checking again.

   The `dirname` parameter specifies the name of the directory containing the movie files.
   The `req` parameter is the request object passed from the Ring handler.
   The `final-name` parameter specifies the name of the final movie file.
   
   Returns: an HTML finished movie page if the movie file exists, otherwise waits and calls itself"

  [dirname req final-name]
  (let [video-file (itv/find-files (str "./resources/public/" dirname) "ALL.mp4")
        condition (= (count video-file) 1)]
    (if condition
      (do
        (println "Movie created.")
        (html/finished-movie-page req dirname final-name))
      (do
        (println "Waiting for sounded movie to be created.")
        (Thread/sleep 1000)
        (get-finished-movie dirname req final-name)))))

(defn finished-page
  "Generates the finished page for the specified directory.
   
   Args:
     req - a request object passed from the Ring handler.

   This function generates the finished page for the specified directory. It first retrieves the
   directory name from the request parameters. It then finds the audio files in the directory,
   sorts them, and generates a final name for the finished movie file. The function merges the
   audios and video into a single file using the `itv/merge-audios-and-video` function. Finally,
   it calls the `get-finished-movie` function to check if the finished movie is ready to be
   displayed.

   The `req` parameter is the request object passed from the Ring handler.
   
   Returns: an HTML finished page displaying the finished movie for the specified directory."
  [req]
  (let [dirname (:resource (:params req))
        audio-files (sort (itv/find-files (str "./resources/public/" dirname) ".au"))
        final-name (str "FINISHED" (System/currentTimeMillis) ".mp4")]
    ;; (Thread/sleep (* 200 (count image-files)))
    (itv/merge-audios-and-video
     (str "./resources/public/" dirname "/ALL.mp4")
     (map #(str "./resources/public/" dirname "/" %) audio-files)
     (str "./resources/public/" dirname "/" final-name))
    (get-finished-movie dirname req final-name)))