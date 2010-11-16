#!/bin/bash

# Must make sure that the Gimp script is in the directory "~/.gimp-2.6/scripts"
cp -r new_icons tinted_icons
cd tinted_icons
gimp -i -b '(flickr-blue-colorize "*.png" 213)' -b '(gimp-quit 0)'
