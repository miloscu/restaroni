(ns itv.core
  (:import [com.googlecode.mp4parser.authoring Movie]
           [com.googlecode.mp4parser.authoring Track]
           [com.googlecode.mp4parser.authoring.builder DefaultMp4Builder]
           [com.googlecode.mp4parser.authoring.container.mp4 MovieCreator]
           [com.googlecode.mp4parser.authoring.tracks AppendTrack]
           [java.io File]
           [java.io FileInputStream]
           [java.io BufferedInputStream]
           [java.io FileOutputStream]
           [javax.imageio ImageIO]
           [javax.sound.sampled AudioSystem]
           [org.bytedeco.ffmpeg.global avcodec]
           [org.bytedeco.ffmpeg.global avutil]
           [org.bytedeco.javacv FFmpegFrameRecorder]
           [org.bytedeco.javacv FFmpegFrameGrabber]
           [org.bytedeco.javacv Java2DFrameConverter]
           [javax.sound.sampled AudioFormat]
           [javax.sound.sampled AudioFormat$Encoding]
           [javax.sound.sampled AudioInputStream]))

(defn conc []
  (let [movie1 (MovieCreator/build "./file.mp4")
        movie2 (MovieCreator/build "./file.mp4")
        movie3 (MovieCreator/build "./file.mp4")
        vet-track-video (into-array Track (filter #(= (:handler %) "vide")
                                                  (flatten (map :tracks [movie1 movie2 movie3]))))
        vet-track-audio (into-array Track (filter #(= (:handler %) "soun")
                                                  (flatten (map :tracks [movie1 movie2 movie3]))))
        movie-output (new Movie)
        list-tracks (list (new AppendTrack vet-track-video) (new AppendTrack vet-track-audio))]
    (.setTracks movie-output list-tracks)
    (let [mp4-builder (new DefaultMp4Builder)
          c (.build mp4-builder movie-output)
          fos (new FileOutputStream "output.mp4")]
      (.writeContainer c (.getChannel fos))
      (.close fos))))

(defn xonc []
  (let [movie1 (MovieCreator/build "./file.mp4")
        movie2 (MovieCreator/build "./file.mp4")
        movie3 (MovieCreator/build "./file.mp4")
        vet-track-video (into-array Track (filter #(= (.getHandler %) "vide")
                                                  (apply concat (flatten (map #(.getTracks %) [movie1 movie2 movie3])))))
        vet-track-audio (into-array Track (filter #(= (.getHandler %) "soun")
                                                  (apply concat (flatten (map #(.getTracks %) [movie1 movie2 movie3])))))
        movie-output (new Movie)
        ;; list-tracks (list (new AppendTrack vet-track-video) (new AppendTrack vet-track-audio))
        ;; list-tracks (list (AppendTrack. (vec vet-track-video)) (AppendTrack. (vec vet-track-audio)))
        ;; list-tracks (list (AppendTrack. (into-array Track vet-track-video)) (AppendTrack. (into-array Track vet-track-audio)))

        vtvat (new AppendTrack vet-track-video)
        vtaat (new AppendTrack vet-track-audio)
        list-tracks (list vtvat vtaat)]
    (.setTracks movie-output list-tracks)
    ;; (println (map #(.toString %) vet-track-audio))
    ;; (map #(.getHandler %) (apply concat (flatten (map #(.getTracks %) [movie1 movie2 movie3]))))
    ;; (apply concat (flatten (map #(.getTracks %) [movie1 movie2 movie3])))
    ;; list-tracks
    (let [mp4-builder (new DefaultMp4Builder)
          c (.build mp4-builder movie-output)
          fos (new FileOutputStream "output.mp4")]
      (.writeContainer c (.getChannel fos))
      (.close fos))))

(defn create [path img-path wav-path width height]
  (let [recorder (FFmpegFrameRecorder. path width height)
        converter (Java2DFrameConverter.)
        img (new File img-path)
        audio-stream (AudioSystem/getAudioInputStream (new File wav-path))]

    (try
      (.setVideoCodec recorder avcodec/AV_CODEC_ID_H264)
      (.setFrameRate recorder 25)
      (.setPixelFormat recorder avutil/AV_PIX_FMT_YUV420P)
      (.setFormat recorder "mp4")

      (.start recorder)
      ;; (doseq [i (range duration)]
      (doseq [i (range 10)]
        (let [image (ImageIO/read img)
              frame (.convert converter image)]
          (println "Recording frame " i)
          (.record recorder frame)))

      (catch Exception e
        (println "Error setting video codec: " e))
      (finally (.stop recorder)
               (.release recorder)))))

;; Function to finf all files of a specific file type in a folder
(defn find-files [folder file-type]
  (let [files (File. folder)
        files-list (.list files)]
    (filter #(re-find (re-pattern file-type) %) files-list)))

(defn xreate [path img-path wav-path width height]
  (let [recorder (FFmpegFrameRecorder. path width height)
        converter (Java2DFrameConverter.)
        img (new File img-path)
        audio-input-stream (AudioSystem/getAudioFileFormat (new File wav-path))
        duration-ms (int (/ (* 1000 (.getFrameLength audio-input-stream))
                            (.getFrameRate (.getFormat audio-input-stream))))
        ;; duration 20
        ]

    ;; duration-ms

    (try
      (.setVideoCodec recorder avcodec/AV_CODEC_ID_H264)
      (.setFrameRate recorder 25)
      (.setVideoBitrate recorder 100)
      (.setPixelFormat recorder avutil/AV_PIX_FMT_YUV420P)
      (.setFormat recorder "mp4")

      (.start recorder)
      (doseq [i (range (/ (float duration-ms) 40))]
        (let [image (ImageIO/read img)
              frame (.convert converter image)]
          (println "Recording frame " i)
          (.record recorder frame)))

      (catch Exception e
        (println "Error setting video codec: " e))
      (finally (.stop recorder)
               (.release recorder)))))

(defn folder-to-movies [folder]
  (let [files (File. folder)
        files-list (.list files)
        files-list (sort (map #(subs % 0 10) (filter #(re-find (re-pattern ".png") %) files-list)))]
    (pmap
     #(xreate
       (str folder "/" % ".mp4")
       (str folder "/" % ".png")
       (str folder "/" % ".au")
       1280
       720)
     files-list)))

(defn merge-audio-and-video [video-path audio-path output-path]
  (let [file (File. video-path)
        is-created true
        grabber1 nil
        grabber2 nil
        recorder nil]
    (if (not (.exists file))
      false
      (try
        (let [grabber1 (new FFmpegFrameGrabber video-path)
              grabber2 (new FFmpegFrameGrabber audio-path)
              recorder (FFmpegFrameRecorder. output-path
                                             1280
                                             720
                                            ;;  (.getImageWidth grabber1)
                                            ;;  (.getImageHeight grabber1)
                                             (.getAudioChannels grabber2))]

          (.setVideoCodec recorder avcodec/AV_CODEC_ID_H264)
          (.setFrameRate recorder 25)
          (.setVideoBitrate recorder 100)
          (.setPixelFormat recorder avutil/AV_PIX_FMT_YUV420P)
          (.setFormat recorder "mp4")

          (println "Audio Channels" (.getAudioChannels grabber2))
          (println "Audio Sample Rate" (.getSampleRate grabber2))
          (println "Audio Sample Format" (.getSampleFormat grabber2))
          (println "Audio Frame Rate" (.getFrameRate grabber2))
          (println "Audio Length in Frames" (.getLengthInFrames grabber2))
          
          
          (println grabber2)
          (.start grabber1)
          (println "here1")
          (.start grabber2)
          (.setFrameRate recorder (.getFrameRate grabber1))
          (.setSampleRate recorder (.getSampleRate grabber2))
          (.start recorder)
          (println "here2")
          (let [frame1 (.grabFrame grabber1)
                frame2 (.grabFrame grabber2)]
            (loop [frame1 (.grabFrame grabber1)]
              (when frame1
                (.record recorder frame1)
                (recur (.grabFrame grabber1))))
            (loop [frame2 (.grabFrame grabber2)]
              (when frame2
                (.record recorder frame2)
                (recur (.grabFrame grabber2))))
            (.stop grabber1)
            (.stop grabber2)
            (.stop recorder)
            is-created))
        (catch Exception e
          (println e))
        (finally
          (try
            (when grabber1
              (.release grabber1))
            (when grabber2
              (.release grabber2))
            (when recorder
              (.release recorder))
            (catch Exception e
              (println e))))))))