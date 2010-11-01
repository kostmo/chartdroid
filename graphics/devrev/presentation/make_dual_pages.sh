#!/bin/bash

cp ~/PDF/slides.pdf notes.pdf

ghostscript \
 -o left-side-outputs.pdf \
 -sDEVICE=pdfwrite \
 -g7920x6120 \
 -dPDFSETTINGS=/prepress \
 -c "<</PageOffset [0 0]>>setpagedevice" \
 -c "<</BeginPage{0.77 0.77 scale}>> setpagedevice" \
 -f notes.pdf

mkdir left
pdftk left-side-outputs.pdf burst output left/page_%02d.pdf


ghostscript \
 -o right-side-outputs.pdf \
 -sDEVICE=pdfwrite \
 -g7920x6120 \
 -dPDFSETTINGS=/prepress \
 -c "<</PageOffset [396 0]>>setpagedevice" \
 -c "<</BeginPage{0.77 0.77 scale}>> setpagedevice" \
 -f notes.pdf

mkdir right
pdftk right-side-outputs.pdf burst output right/page_%02d.pdf

mkdir both
for (( c=0; c<24; c++ ))
do
	DOUBLE=$((2 * $c))
	NUM1=`printf "%02d" $((1 + $DOUBLE))`
	NUM2=`printf "%02d" $((2 + $DOUBLE))`

	echo "Combining $NUM1 and $NUM2"

	OUTNUM=`printf "%02d" $c`

	pdftk left/page_$NUM1.pdf stamp right/page_$NUM2.pdf output both/out_$OUTNUM.pdf
done

pdftk both/*.pdf cat output combined.pdf
rm -r left right both left-side-outputs.pdf right-side-outputs.pdf doc_data.txt
