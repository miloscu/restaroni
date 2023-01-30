(ns tts.core
  (:import [com.sun.speech.freetts VoiceManager]
           [com.sun.speech.freetts Voice]
           [com.sun.speech.freetts.audio AudioPlayer]
           [com.sun.speech.freetts.audio SingleFileAudioPlayer]
           [javax.sound.sampled AudioFileFormat$Type]))


;; (defn tts-to-file [text filename]
;;   (let [voice-manager (VoiceManager/getInstance)
;;         voice (.getVoice voice-manager "kevin16")
;;         audio-player (SingleFileAudioPlayer. filename false)]
;;     (.setAudioPlayer voice audio-player)
;;     (.allocate voice)
;;     (.synthesizeToFile voice text filename)
;;     (.deallocate voice)
;;     (.close audio-player)))

;; (tts-to-file "Hello, world!" "hello.wav")

(defn speakaroni [path text _i voice]
  (let [audioPlayer (new SingleFileAudioPlayer path AudioFileFormat$Type/AU)]
    (try
      ;; (Thread/sleep (* wait 100))
      (.setAudioPlayer voice audioPlayer)
      (.speak voice text)
      (.close audioPlayer)
      (catch Exception e (println "err")))))