#!/bin/bash

KEYS_DIR=~/android/keys/taxotrack
FLICKR_API_KEYS_FILENAME=ApiKeys.java
FLICKR_API_KEYS_PROJECT_PATH=browser/src/org/crittr/keys/$FLICKR_API_KEYS_FILENAME

TEMP_DIRECTORY=temp

mkdir $TEMP_DIRECTORY

cp $FLICKR_API_KEYS_PROJECT_PATH $TEMP_DIRECTORY
cp $KEYS_DIR/$FLICKR_API_KEYS_FILENAME $FLICKR_API_KEYS_PROJECT_PATH

cd browser
ant release
cp bin/TaxoBrowser-release.apk ../
#ant clean
cd ..

# Restore the dummy API keys file
cp $TEMP_DIRECTORY/$FLICKR_API_KEYS_FILENAME $FLICKR_API_KEYS_PROJECT_PATH
rm -r $TEMP_DIRECTORY


echo -n "[u]pdate, [f]resh reinstall, or [n]either? [U/f/n]: "
read character
case $character in
    [Ff] ) echo "Will now uninstall then reinstall."
        adb uninstall org.crittr.browse
        adb install TaxoBrowser-release.apk
        ;;
    [Uu] | "" ) echo "Will now update the app."
        adb install -r TaxoBrowser-release.apk
        ;;
    * ) echo "Fine, then."
esac
