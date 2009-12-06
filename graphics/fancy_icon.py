#!/usr/bin/env python

import os

input_filename = "logo.png"
output_filename = "fancy.png"


# shadow parameters: blur opacity (percent), blur radius (pixels), x-offset, y-offset
skew_dropshadow = "convert %s -background none -shear 0x10 \( +clone -shadow 30x15+30 \) +swap -background none -layers merge +repage -trim %s" % (input_filename, output_filename)

os.system( skew_dropshadow )

import Image
im = Image.open( output_filename )
width, height = im.size

