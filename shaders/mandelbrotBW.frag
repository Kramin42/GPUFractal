// Fragment program
#version 150
#extension GL_ARB_gpu_shader_fp64 : enable

#define PERIODICITY_CHECKING_THRESHHOLD 0.000000000000001 //carefull with this value; it can lead to inaccuracy

uniform dvec2 center;
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
	int iteration = 0;
	vec3 colour = vec3(0.0,0.0,0.0);
	
	//for periodicity checking
	int period = 8;
	int periodCounter = 0;
	dvec2 prevz = z;
	
	bool foundPeriod = false;//for testing
	
	float zlen = length(vec2(z));
	float minz = zlen;
	int minziter = 0;
	
	while (iteration<maxIteration && zlen < 3.0) {
		//periodicity checking (can halve the render time when there are large black areas)
		if (periodCounter >= period){
			if (length(vec2(z-prevz)) < PERIODICITY_CHECKING_THRESHHOLD){
				iteration = maxIteration;
				//foundPeriod = true;
				break;
			}
			periodCounter = 0;
      period+=period;
			prevz = z;
		}
		
		if (zlen < minz){
		  minziter = iteration;
		  minz = zlen;
		}
		
		// do z = z^2 + c
		z = dvec2(z.x*z.x-z.y*z.y,2.0*z.x*z.y) + c;
		
		zlen = length(vec2(z));
		
		iteration++;
		periodCounter++;
	}
	
	//colour = vec3(float(iteration)/maxIteration,0.0,0.0);
    
	if (iteration < maxIteration){
		//smooth colouring
		float mu = float(iteration+1-minIteration) - log(log(length(vec2(z))))/0.6931471805599453; //log(2.0) = 0.6931471805599453
    mu = max(mu,0.0);
		//transform to between 0 and 1
		//mu/(mu+constant) goes to 1 as mu goes to infinity and to 0 as mu goes to 0.
		// This transformation is much better than a simple mu = mu/maxIteration because it 
		// is independent of maxIteration and it reduces the 1/x like behaviour of mu as you move away from the boundary of the set.
		mu=mu/(mu+transformParam);
		
		//colour.x = getColourValue(mu,0.4,0.5,1.0);
		//colour.y = getColourValue(mu,0.3,0.5,0.7);
		//colour.z = getColourValue(mu,0.0,0.5,0.6);
		
		colour = vec3(1.0-mu, 1.0-mu, 1.0-mu);
	}
	else {
	  float mu = float(minziter);
	  mu = 0.5*10.0/(mu+10.0)+0.5*sqrt(minz);//sqrt(minz) adds a little gradient to the interior 
	  //colour.x = 8.0*minz*minz;
	  colour.x = mu;
	  colour.y = colour.x;
	  colour.z = colour.x;
	}
	
	
  outputF = vec4( colour.x, colour.y, colour.z, 1.0 );
	//outputF = vec4( 0.0, 1.0, 0.0, 1.0 );
}
