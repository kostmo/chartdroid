#!/bin/bash

convert $1 +matte \( +clone -shade 110x90 -normalize -negate +clone -compose Plus -composite \) \( -clone 0 -shade 110x50 -normalize -channel BG -fx 0 +channel -matte \) -delete 0 +swap -compose Multiply -composite z.png
