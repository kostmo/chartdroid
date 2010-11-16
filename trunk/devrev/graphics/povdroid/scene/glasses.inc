/*
    Brille
*/
//
// Includes 
//
#include "colors"
#include "shapes"
#include "textures"
//
// Objectdefinitionen
//
declare Linse=
  intersection{
    sphere{<0,-5,0>, 8}
    sphere{<0,5,0>, 8}
    scale 0.16
    texture{Glass3
    	pigment {
    		Black
    		transmit 0.7
    	}
    	
    }
    finish{ambient 0.3}}

#declare Gestell=
  union{
    difference{
      torus{ 5, 0.3}
      cylinder{ <0,-1,0>, <0,1,0>, 4.7}
      scale <1,1,0.7>
      translate -7*x}
    difference{
      torus{ 5, 0.3}
      cylinder{ <0,-1,0>, <0,1,0>, 4.7}
      scale <1,1,0.7>
      translate 7*x}
    difference{
      torus{ 3, 0.3
             scale <0.9,1,1>}
      box{ <-5,-1,-5>, <5,1,0>}
      cylinder{ <0,-1,0>, <0,1,0>, 5
                scale <1,1,0.7>
                translate -7*x}
      cylinder{ <0,-1,0>, <0,1,0>, 5
                scale <1,1,0.7>
                translate 7*x}}
    cylinder{<-12,0,0>, <-12,-15,0>, 0.3}
    cylinder{<12,0,0>, <12,-15,0>, 0.3}
    union{
      intersection{
        torus{5, 0.3}
        box{<0,-1,0>, <8,1,6>}}
      sphere{<5,0,0>, 0.3}
      scale <1.5,1,1>
      rotate -90*z
      translate <-12,-15,-5>}
    union{
      intersection{
        torus{5, 0.3}
        box{<0,-1,0>, <8,1,6>}}
      sphere{<5,0,0>, 0.3}
      scale <1.5,1,1>
      rotate -90*z
      translate <12,-15,-5>}
    pigment{White}
    finish{ambient 0.5}}

#declare Brille=
  union{
    object{Gestell}
    object{Linse
           scale <5,1,5*0.7>
           translate -7*x}
    object{Linse
           scale <5,1,5*0.7>
           translate 7*x}}
