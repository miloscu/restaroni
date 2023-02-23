# restaroni

TODO:
 - Fix red tint (find pixel format which both works and works correctly (right now we use Y′UV420p which works but tints the entire video a reddish hue))
 - Style (make the jFX images a bit more similar to how actual Reddit looks)
 - Improve speech (requires interop with Balabolka (windows) or similar (linux)). This repo is a POC that this can be done in pure JVM.
 - Fiddle around with video bitrate

A web app and toolkit to create spoken word videos out of top-level replies to a thread.

Contains a derivative of https://github.com/ThatGuyHughesy/creddit licenced under the MIT licence. See their repo and /src/creddit for comparison.

## Usage

Register an app on Redit
https://www.reddit.com/prefs/apps

run (start-server) in the main namespace or run the .jar you built.

Enter your App ID, App Secret, the thread itself and the number of comments you want to get from the thread (will default to all if the count exceeds the actual comment count)

Follow the steps for OLD (semi-working (works until the last step)) and NEW (fully-working) to create your video.

## License

CC0
https://creativecommons.org/publicdomain/zero/1.0/

No Copyright
The person who associated a work with this deed has dedicated the work to the public domain by waiving all of his or her rights to the work worldwide under copyright law, including all related and neighboring rights, to the extent allowed by law.

You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission. See Other Information below.

Dependencies under their respective licenses.

Contains a derivative of https://github.com/ThatGuyHughesy/creddit licenced under the MIT licence.