#!/bin/bash

KEYS_DIR=~/android/keys/taxotrack
GOOGLE_MAP_API_KEY_FILENAME=strings_map.xml
GOOGLE_MAP_API_KEY_PROJECT_PATH=tracker/res/values/$GOOGLE_MAP_API_KEY_FILENAME

TEMP_DIRECTORY=temp

mkdir $TEMP_DIRECTORY

cp $GOOGLE_MAP_API_KEY_PROJECT_PATH $TEMP_DIRECTORY
cp $KEYS_DIR/$GOOGLE_MAP_API_KEY_FILENAME $GOOGLE_MAP_API_KEY_PROJECT_PATH

cd tracker
ant release
cp bin/TaxoTracker-release.apk ../
#ant clean
cd ..

# Restore the dummy API keys file
cp $TEMP_DIRECTORY/$GOOGLE_MAP_API_KEY_FILENAME $GOOGLE_MAP_API_KEY_PROJECT_PATH
rm -r $TEMP_DIRECTORY


echo -n "[u]pdate, [f]resh reinstall, or [n]either? [U/f/n]: "
read character
case $character in
    [Ff] ) echo "Will now uninstall then reinstall."
        adb uninstall org.crittr.track
        adb install TaxoTracker-release.apk
        ;;
    [Uu] | "" ) echo "Will now update the app."
        adb install -r TaxoTracker-release.apk
        ;;
    * ) echo "Fine, then."
esac

