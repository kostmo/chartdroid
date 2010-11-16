#!/bin/bash

KEYS_DIR=~/android/keys/flickr
FLICKR_API_KEYS_FILENAME=ApiKeys.java
FLICKR_API_KEYS_PROJECT_PATH=app/src/com/kostmo/flickr/keys/$FLICKR_API_KEYS_FILENAME


GOOGLE_MAP_API_KEY_FILENAME=strings_map.xml
GOOGLE_MAP_API_KEY_PROJECT_PATH=app/res/values/$GOOGLE_MAP_API_KEY_FILENAME

TEMP_DIRECTORY=temp

mkdir $TEMP_DIRECTORY
cp $FLICKR_API_KEYS_PROJECT_PATH $TEMP_DIRECTORY
cp $KEYS_DIR/$FLICKR_API_KEYS_FILENAME $FLICKR_API_KEYS_PROJECT_PATH

cp $GOOGLE_MAP_API_KEY_PROJECT_PATH $TEMP_DIRECTORY
cp $KEYS_DIR/$GOOGLE_MAP_API_KEY_FILENAME $GOOGLE_MAP_API_KEY_PROJECT_PATH

cd app
ant release
cp bin/OpenFlickr-release.apk ../
ant clean
cd ..

# Restore the dummy API keys file
cp $TEMP_DIRECTORY/$FLICKR_API_KEYS_FILENAME $FLICKR_API_KEYS_PROJECT_PATH
cp $TEMP_DIRECTORY/$GOOGLE_MAP_API_KEY_FILENAME $GOOGLE_MAP_API_KEY_PROJECT_PATH
rm -r $TEMP_DIRECTORY


echo -n "Do you want to install the new .apk on your device? [Y/n]: "
read character
case $character in
    [Yy] | "" ) echo "You responded in the affirmative."
        adb uninstall com.kostmo.flickr.bettr
        adb install OpenFlickr-release.apk
        ;;
    * ) echo "Fine, then."
esac
