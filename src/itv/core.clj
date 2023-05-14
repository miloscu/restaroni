(ns itv.core
  (:import [com.googlecode.mp4parser.authoring Movie]
           [com.googlecode.mp4parser.authoring Track]
           [com.googlecode.mp4parser.authoring.builder DefaultMp4Builder]
           [com.googlecode.mp4parser.authoring.builder FragmentedMp4Builder]
           [com.googlecode.mp4parser.authoring.container.mp4 MovieCreator]
           [com.googlecode.mp4parser.authoring.tracks AppendTrack]
           [java.io File]
           [java.io FileInputStream]
           [java.io BufferedInputStream]
           [java.io FileOutputStream]
           [java.io RandomAccessFile]
           [javax.imageio ImageIO]
           [javax.sound.sampled AudioSystem]
           [org.bytedeco.ffmpeg.global avcodec]
           [org.bytedeco.ffmpeg.global avutil]
           [org.bytedeco.javacv FFmpegFrameRecorder]
           [org.bytedeco.javacv FFmpegFrameGrabber]
           [org.bytedeco.javacv Java2DFrameConverter]
           [javax.sound.sampled AudioFormat]
           [javax.sound.sampled AudioFormat$Encoding]
           [javax.sound.sampled AudioInputStream])
  (:require [clojure.string :as str]))

;; Function to finf all files of a specific file type in a folder
(defn find-files [folder file-type]
  (let [files (File. folder)
        files-list (.list files)]
    (filter #(re-find (re-pattern file-type) %) files-list)))

(defn xreate-all [path img-paths wav-paths width height]
  (let [recorder (FFmpegFrameRecorder. path width height)
        converter (Java2DFrameConverter.)
        imgs (map #(new File %) img-paths)
        audio-input-streams (map #(AudioSystem/getAudioFileFormat (new File %)) wav-paths)
        duration-ms-arr (map #(int (/ (* 1000 (.getFrameLength %))
                                      (.getFrameRate (.getFormat %)))) audio-input-streams)]

    (println "duration-ms-arr: " duration-ms-arr)

    (try
      (.setVideoCodec recorder avcodec/AV_CODEC_ID_H264)
      (.setFrameRate recorder 25)
      (.setVideoBitrate recorder 200)
      (.setPixelFormat recorder avutil/AV_PIX_FMT_YUV420P)
   ;; (.setPixelFormat recorder avutil/AV_PIX_FMT_RGB32_1)
      (.setFormat recorder "mp4")

      (.start recorder)
      (doseq [j (range (count imgs))]
        (let [img (nth imgs j)
              duration-ms (nth duration-ms-arr j)]
          (doseq [i (range (/ (float duration-ms) 40))]
            (let [image (ImageIO/read img)
                  frame (.convert converter image)]
              (println "Recording frame " i)
              (.record recorder frame)))))

      (catch Exception e
        (println path img-paths wav-paths))
      (finally (.stop recorder)
               (.release recorder)))))

(defn folder-to-movies2 [folder]
  (let [files (File. folder)
        files-list (.list files)
        files-list (sort (map #(subs % 0 10) (filter #(re-find (re-pattern ".png") %) files-list)))]
    (println (map #(str folder "/" % ".au") files-list))
    (xreate-all
     (str folder "/ALL.mp4")
     (map #(str folder "/" % ".png") files-list)
     (map #(str folder "/" % ".au") files-list)
     1920
     1080)))

(defn merge-audios-and-video [video-path audio-paths output-path]
  (let [file (File. video-path)
        is-created true
        video-grabber nil
        audio-grabbers []
        recorder nil]
    (if (not (.exists file))
      false
      (try
        (let [video-grabber (new FFmpegFrameGrabber video-path)
              audio-grabbers (map #(new FFmpegFrameGrabber %) audio-paths)
              recorder (FFmpegFrameRecorder. output-path 1920 1080 (.getAudioChannels (first audio-grabbers)))]

          (println video-path)
          (println audio-paths)

          (doseq [audio-grabber audio-grabbers]
            (.start audio-grabber))
          (.start video-grabber)

          (.setFrameRate recorder (.getFrameRate video-grabber))
          (.setSampleRate recorder (.getSampleRate (first audio-grabbers)))
          (.setAudioChannels recorder (.getAudioChannels (first audio-grabbers)))
          (.setFormat recorder "mp4")
          (.setVideoCodec recorder avcodec/AV_CODEC_ID_H264)
          (.setFrameRate recorder 25)
          (.setVideoBitrate recorder 200000)
          (.setAudioBitrate recorder 128000)
          (.setPixelFormat recorder avutil/AV_PIX_FMT_YUV420P)
       ;; (.setPixelFormat recorder avutil/AV_PIX_FMT_RGB32_1)
          (.setAudioCodec recorder avcodec/AV_CODEC_ID_AAC)

          (.start recorder)

          (let [frame1 (.grabFrame video-grabber)
                frame2 (.grabFrame (first audio-grabbers))]
            (println frame1)
            (println frame2)
            (loop [frame1 (.grabFrame video-grabber)]
              (when frame1
                (.record recorder frame1)
                (recur (.grabFrame video-grabber))))
            (doseq [audio-grabber audio-grabbers]
              (loop [frame2 (.grabFrame audio-grabber)]
                (when frame2
                  (.record recorder frame2)
                  (recur (.grabFrame audio-grabber)))))

            (.stop video-grabber)
            (doseq [audio-grabber audio-grabbers]
              (.stop audio-grabber))
            (.stop recorder)
            is-created))
        (catch Exception e
          (println e))))))