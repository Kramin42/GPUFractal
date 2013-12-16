// Fragment program
#version 150
#extension GL_ARB_gpu_shader_fp64 : enable

#define PERIODICITY_CHECKING_THRESHHOLD 0.000000000000001 //carefull with this value; it can lead to inaccuracy

uniform vec2 center;
uniform float invzoom;
uniform int maxIteration;
uniform int minIteration;
uniform float transformParam;

in vec2 pos;

out vec4 outputF;

float getColourValue(float mu, float s, float m, float e)
{
    if (mu<s) {
        return 0.0;
    } else if (mu<m) {
        return (mu - s)/(m-s);
    } else if (mu<e) {
        return 1.0 - (mu - m)/(e-m);
    }
    return 0.0;
}

void main( void ) {
	dvec2 c = dvec2(center) + dvec2(pos)*invzoom;
	dvec2 z = c;
	dvec2 dz = dvec2(1.0,0.0);
	int iteration = 0;
	vec3 colour = vec3(0.0,0.0,0.0);
	
	//for periodicity checking
	int period = 8;
	int periodCounter = 0;
	dvec2 prevz = z;
	//dvec2 prevdz = dz;
	
	while (iteration<maxIteration && length(vec2(z.x,z.y)) < 3.0) {
		//periodicity checking (can halve the render time when there are large black areas)
		if (periodCounter >= period){
			if (length(vec2(z-prevz)) < PERIODICITY_CHECKING_THRESHHOLD){
				iteration = maxIteration;
				break;
			}
			periodCounter = 0;
      period+=period;
			prevz = z;
		}
		
		// do dz = 2.z.dz + 1
		dz = dvec2(1.0+2*(z.x*dz.x-z.y*dz.y),2*(z.x*dz.y+z.y*dz.x));
		
		// do z = z^2 + c
		z = dvec2(z.x*z.x-z.y*z.y,2.0*z.x*z.y) + c;
		
		iteration++;
		//periodCounter++;
	}
	
	//colour = vec3(float(iteration)/maxIteration,0.0,0.0);
    
	
	if (iteration < maxIteration){
	  //do a few more iterations
	  for (int i=0; i<2; i++){
	    // do dz = 2.z.dz + 1
		  dz = dvec2(1.0+2.0*(z.x*dz.x-z.y*dz.y),2*(z.x*dz.y+z.y*dz.x));
		  
		  // do z = z^2 + c
		  z = dvec2(z.x*z.x-z.y*z.y,2.0*z.x*z.y) + c;
	  }
	  float zlen = length(vec2(z.x,z.y));
	  float mu = 2.0*log(zlen)*(zlen/length(vec2(dz.x,dz.y)));
	  
		//smooth colouring
		//transform mu to between 0 and 1
		//mu=0.005/(mu+0.005) + 0.1;
		
		//colour.x = getColourValue(mu,0.4,0.5,1.0);
		//colour.y = getColourValue(mu,0.3,0.5,0.7);
		//colour.z = getColourValue(mu,0.0,0.5,0.6);
		
		mu = 1000*mu;
		colour = vec3(mu, mu, mu);
	}
	
	
  outputF = vec4( colour.x, colour.y, colour.z, 1.0 );
	//outputF = vec4( 0.0, 1.0, 0.0, 1.0 );
}
