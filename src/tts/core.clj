(ns tts.core
  (:import [com.sun.speech.freetts VoiceManager]
           [com.sun.speech.freetts Voice]
           [com.sun.speech.freetts.audio AudioPlayer]
           [com.sun.speech.freetts.audio SingleFileAudioPlayer]
           [javax.sound.sampled AudioFileFormat$Type]))

(defn speakaroni [path text _i voice]
  (let [wavAudioPlayer (new SingleFileAudioPlayer path AudioFileFormat$Type/WAVE)
        auAudioPlayer (new SingleFileAudioPlayer path AudioFileFormat$Type/AU)]
    (try
      ;; (Thread/sleep (* wait 100))
      (.setAudioPlayer voice wavAudioPlayer)
      (.speak voice text)
      (.close wavAudioPlayer)
      (.setAudioPlayer voice auAudioPlayer)
      (.speak voice text)
      (.close auAudioPlayer)
      (catch Exception e (println "err")))))