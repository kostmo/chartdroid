// Author: Karl Ostmo
// Date: 5/17/2010
 
#include "colors.inc"
#include "metals.inc"
 
#include "glasses.inc"
 
// See http://www.android.com/branding.html

#declare OVERLAP_MARGIN = 10;

#declare antennae_angle = 30;
#declare torso_radius = 0.5;
#declare antennae_length = torso_radius*3/5;
#declare torso_length = torso_radius*7/5;
#declare torso_top = 0.6;
#declare torso_bottom = torso_top - torso_length;
#declare leg_length = torso_length/4;


#declare eye_horizontal_displacement = torso_radius/2;
#declare leg_horizontal_displacement = eye_horizontal_displacement;
#declare antenna_horizontal_displacement = eye_horizontal_displacement;
#declare eye_socket_depth = torso_radius;

#declare arm_radius = torso_radius/5;
#declare leg_radius = arm_radius;
#declare eye_radius = arm_radius/2;
#declare antenna_radius = arm_radius/5;
#declare necklace_radius = antenna_radius;
#declare body_edge_radius = arm_radius;
#declare dollar_extrusion_radius = torso_radius + torso_radius/OVERLAP_MARGIN;

#declare eyelevel_height = torso_top + torso_radius/2;
#declare head_separation = eye_radius;
#declare arm_horizontal_displacement = torso_radius + arm_radius;



light_source {
	x*torso_length*6, rgb 1
	rotate z*30
/*	rotate y*45	*/
}
 
camera {
	perspective
	location <4*torso_length, eyelevel_height + torso_length/2, -torso_length>
/*	location <4*torso_length, 0, 0>	*/
	sky y
	direction z
	right x*3/4
	up y
	
	look_at y*eyelevel_height
/*	look_at 0	*/

/*	
   rotate z*90
   rotate x*-90
*/
}

#declare Antenna = cylinder {
	y*antennae_length, 0, antenna_radius
}
 
#declare Arm = merge {
	sphere {
		0, arm_radius
		translate y*(torso_top - arm_radius)
	}
 
	cylinder {
		y*(torso_top - arm_radius), y*arm_radius, arm_radius
	}
 
	sphere {
		y*arm_radius, arm_radius
	}
}
 
#declare Leg = merge {
	cylinder {
		y*(torso_top - leg_radius), y*(torso_bottom - leg_length), leg_radius
		translate y*(-antenna_horizontal_displacement)
	}
	sphere {
		y*(torso_bottom - leg_length), leg_radius
		translate y*(-antenna_horizontal_displacement)
	}
}
 
 
#declare EyeSocket = cylinder {
	y*eye_socket_depth, 0, eye_radius
	rotate z*(-90)
	translate y*eyelevel_height
}

#declare TorsoCylinder = cylinder {
	y*torso_top, y*torso_bottom, torso_radius
}

#declare DollarCylinder = cylinder {
	y*torso_top, y*torso_bottom, dollar_extrusion_radius
}


#declare myText = text {
	ttf "crystal.ttf", "$", torso_radius, 0
	scale torso_length*1.2
	scale x*1.5
}

#declare myTextMin = min_extent(myText);
#declare myTextMax = max_extent(myText);

#declare DollarSign = intersection {
	
	object { myText
		rotate y*-90
	
		translate <0,
			(torso_top + torso_bottom)/2 - (myTextMax.y - myTextMin.y)/2,
			-(myTextMax.x - myTextMin.x)/2>
		translate x*(dollar_extrusion_radius)
	}
	
	object { DollarCylinder }
}

#declare Android = merge {

 
	merge {
		object{ Arm
			translate z*(-arm_horizontal_displacement)
		}
 
		object{ Arm
			translate z*(arm_horizontal_displacement)
		}
	}
 
	merge {
		object{ Leg
			translate z*(leg_horizontal_displacement)
		}
 
		object{ Leg
			translate z*(-leg_horizontal_displacement)
		}
	}
 
 	object {
 		// Central body (including torso, head, eyes, antennae)
	
		merge {
			difference {
		
				// Head and Torso
				merge {
		
					// Head 
					sphere {
						y*torso_top, torso_radius
					}
		
					// Torso
					object {TorsoCylinder}
		 
					// Rounded torso bottom ("skirt")
					torus {
						torso_radius - body_edge_radius, body_edge_radius
						translate y*torso_bottom
					}
		 
					cylinder {
						y*(torso_bottom + torso_length/OVERLAP_MARGIN), y*(torso_bottom - body_edge_radius), torso_radius - body_edge_radius
					}
				}
		 
				cylinder {
					// Head-torso spacing
					y*(torso_top + head_separation), y*torso_top, torso_radius + torso_radius/OVERLAP_MARGIN
				}
		 
				merge {
					// Eye sockets
					object{	EyeSocket
						translate -z*eye_horizontal_displacement
					}
					object{	EyeSocket
						translate z*eye_horizontal_displacement
					}
				}
				
		 	}
		 	
			merge {
				object{ Antenna
					rotate x*(-antennae_angle)
					translate <0, torso_top + antennae_length, -antenna_horizontal_displacement>
				}
		 
				object{ Antenna
					rotate x*(antennae_angle)
					translate <0, torso_top + antennae_length, antenna_horizontal_displacement>
				}
			}
		}
		
		rotate y*clock*360
	}

	// Android Green is: #A4C639 or <0.64, 0.78, 0.22>
	pigment {
		White
	}
}

#declare necklace_tilt_angle = -12;

merge {
	object {Android}
	object {DollarSign
	
		texture {
			finish {
				F_MetalD
	
				ambient 0.2
				diffuse 0.8
			}
			
			
			pigment {
				Green
			}		
			
			normal {
				granite
				turbulence 2
				scale .1
			}
		}
	}
	object {
		torus {
			torso_radius + necklace_radius, necklace_radius
			rotate z*necklace_tilt_angle
			scale <1/cos(necklace_tilt_angle*pi/180), 1, 1>
		}	
		translate y*((torso_top - torso_bottom)*19/20 + torso_bottom)

		pigment {
			Yellow
		}		

		finish {
			F_MetalD	
			ambient 0.2
			diffuse 0.6
		}
	}
	
	/*
	object {
		object {
			Brille
			scale 0.035
			rotate <-90,-90,0>
		}
		translate x*torso_radius
		translate y*eyelevel_height
	}
	*/
}
