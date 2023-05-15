# restaroni

A tool and web app to create spoken word videos from top-level replies to a Reddit thread.

# Usage

Video instruction here: https://www.youtube.com/watch?v=b1XTv1_v7aE

Register an app on Redit
https://www.reddit.com/prefs/apps

run (start-server) in the main namespace or run the .jar you built.

## STEP 1

Enter your App ID, App Secret, the thread itself and the number of comments you want to get from the thread (will default to all if the count exceeds the actual comment count)
Example: 
- 3Nb6bpRn7uTjgA
- 48uwu2IIZp5t6CPxl3SzT1_6hEubww
- https://www.reddit.com/r/AskReddit/comments/135hflq/what_country_do_you_dream_of_living_in_and_why/
- 10

The controller for the POST request you make will use the Reddit API wrapper made by ThatGuyHughesy and extended by me. It will return the listing whose link you pasted and display it as the title and a list of cards representing the top-level comments. It will also create the images and audio preemptively so you can go to

## STEP 2

Clicking GET IMAGES AND AUDIO will fetch the images and audio created in step 1 from the resource folder and lay them out for you.


## STEP 3

Clicking GENERATE SILENT MOVIE (NEW) will use the images and lengths of the audio files to create a silent slideshow with each slide lasting as long as necessary for


## STEP 4

Clicking APPEND AUDIO will append the audio files from step 3 to the slideshow you made and take you to 


## STEP 5

Which is the finished video you are free to download.

### NB! _You are free to explore the (OLD) scenario as well_


# TODO:
 - Fix red tint (find pixel format which both works and works correctly (right now we use Y′UV420p which works but tints the entire video a reddish hue))
 - Style (make the jFX images a bit more similar to how actual Reddit looks)
 - Improve speech (requires interop with Balabolka (windows) or similar (linux)). This repo is a POC that this can be done in pure JVM.
 - Fiddle around with video bitrate

# Acknowledgements
Contains a derivative of https://github.com/ThatGuyHughesy/creddit licenced under the MIT licence. See their repo and /src/creddit for comparison.

Essentially, I used their code in the controller to initialize the client, and then added a listing GET from the reddit docs  https://www.reddit.com/dev/api/#GET_by_id_{names} which I use in the listing_transformer namespace.
All further source* code in all other namespaces is original. 

*Code comments were written using GPT3.5

# License

CC0
https://creativecommons.org/publicdomain/zero/1.0/

No Copyright
The person who associated a work with this deed has dedicated the work to the public domain by waiving all of his or her rights to the work worldwide under copyright law, including all related and neighboring rights, to the extent allowed by law.

You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission. See Other Information below.

Dependencies under their respective licenses.

Contains a derivative of https://github.com/ThatGuyHughesy/creddit licenced under the MIT licence.