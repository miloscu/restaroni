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

(defn find-files
  "Finds files of a specified file type in the given folder.

  Args:
    folder      - A string representing the folder path.
    file-type   - A string representing the file type to search for.

  Returns:
    A lazy sequence of file names that match the specified file type."

  [folder file-type]
  (let [files (File. folder)
        files-list (.list files)]
    (filter #(re-find (re-pattern file-type) %) files-list)))

(defn create-all
  "Creates a video by combining multiple images and audio files.

  Args:
    path        - A string representing the output video file path.
    img-paths   - A sequence of strings representing the paths of input image files.
    wav-paths   - A sequence of strings representing the paths of input audio files.
    width       - An integer representing the width of the output video.
    height      - An integer representing the height of the output video.

  Returns:
    None."

  [path img-paths wav-paths width height]
  (let [recorder (FFmpegFrameRecorder. path width height)
        converter (Java2DFrameConverter.)
        imgs (map #(new File %) img-paths)
        audio-input-streams (map #(AudioSystem/getAudioFileFormat (new File %)) wav-paths)

        ;; Calculate the duration in milliseconds for each audio file
        duration-ms-arr (map #(int (/ (* 1000 (.getFrameLength %))
                                      (.getFrameRate (.getFormat %)))) audio-input-streams)]

    (try
      (.setVideoCodec recorder avcodec/AV_CODEC_ID_H264)
      (.setFrameRate recorder 25)
      (.setVideoBitrate recorder 200)
      (.setPixelFormat recorder avutil/AV_PIX_FMT_YUV420P)
      (.setFormat recorder "mp4")
      (.start recorder)

      ;; Process each image
      (doseq [j (range (count imgs))]
        (let [img (nth imgs j)
              duration-ms (nth duration-ms-arr j)]

          ;; Process each frame for the image
          (doseq [i (range (/ (float duration-ms) 40))]
            (let [image (ImageIO/read img)
                  frame (.convert converter image)]
              (println "Recording frame " i)
              (.record recorder frame)))))

      (catch Exception e
        (println path img-paths wav-paths))

      ;; Ensure recorder is stopped and released
      (finally
        (.stop recorder)
        (.release recorder)))))

(defn folder-to-movies2
  "Converts a folder containing PNG images and corresponding AU audio files
  into a single video file.

  Args:
    folder  - A string representing the path to the folder containing the images and audio files.

  Returns:
    None."

  [folder]
  (let [files (File. folder)
        files-list (.list files)
        files-list (sort (map #(subs % 0 10) (filter #(re-find (re-pattern ".png") %) files-list)))]
    (println (map #(str folder "/" % ".au") files-list))
    (create-all
     (str folder "/ALL.mp4")
     (map #(str folder "/" % ".png") files-list)
     (map #(str folder "/" % ".au") files-list)
     1920
     1080)))

(defn merge-audios-and-video
  "Merges multiple audio files with a video file and saves the result to an output file.

  Args:
    video-path    - A string representing the path to the video file.
    audio-paths   - A sequence of strings representing the paths to the audio files.
    output-path   - A string representing the output file path.

  Returns:
    Boolean indicating whether the merging process was successful (true) or not (false)."

  [video-path audio-paths output-path]
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

          ;; Set properties for the output video file
          (.setFrameRate recorder (.getFrameRate video-grabber))
          (.setSampleRate recorder (.getSampleRate (first audio-grabbers)))
          (.setAudioChannels recorder (.getAudioChannels (first audio-grabbers)))
          (.setFormat recorder "mp4")
          (.setVideoCodec recorder avcodec/AV_CODEC_ID_H264)
          (.setFrameRate recorder 25)
          (.setVideoBitrate recorder 200000)
          (.setAudioBitrate recorder 128000)
          (.setPixelFormat recorder avutil/AV_PIX_FMT_YUV420P)
          (.setAudioCodec recorder avcodec/AV_CODEC_ID_AAC)

          (.start recorder)

          ;; Process frames from video and audio files
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

            ;; Stop the grabbers and recorder
            (.stop video-grabber)
            (doseq [audio-grabber audio-grabbers]
              (.stop audio-grabber))
            (.stop recorder)
            is-created))
        (catch Exception e
          (println e))))))