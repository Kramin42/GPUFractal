// Fragment program
#version 150
#extension GL_ARB_gpu_shader_fp64 : enable

uniform dvec2 center;
uniform float invzoom;
uniform int maxIteration;
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
	
	float zlen = length(vec2(z));
	float minz = zlen;
	int minziter = 0;
	
	while (iteration<maxIteration && zlen < 3000.0) {
		if (zlen < minz){
		  minziter = iteration;
		  minz = zlen;
		}
		
		// do z = z^2 + c
		z = dvec2(z.x*z.x-z.y*z.y,2.0*z.x*z.y) + c;
		
		zlen = length(vec2(z));
		
		iteration++;
	}
    
	if (iteration < maxIteration){
		//smooth colouring
		float mu = float(iteration) - log(log(length(vec2(z))))/0.6931471805599453; //log(2.0) = 0.6931471805599453
    mu = max(mu,0.0);
		//transform to between 0 and 1
		//mu/(mu+constant) goes to 1 as mu goes to infinity and to 0 as mu goes to 0.
		//This transformation is much better than a simple mu = mu/maxIteration because it 
		//is independent of maxIteration
		mu=mu/(mu+transformParam);
		
		colour.x = getColourValue(mu,0.5,1.0,1.0) + getColourValue(mu,0.0,0.5,1.0)*0.867 + getColourValue(mu,-1.0,-0.5,0.5)*0.302;
		colour.y = getColourValue(mu,0.5,1.0,1.0) + getColourValue(mu,0.0,0.5,1.0)*0.282 + getColourValue(mu,-1.0,-0.5,0.5)*0.114;
		colour.z = getColourValue(mu,0.5,1.0,1.0) + getColourValue(mu,0.0,0.5,1.0)*0.078 + getColourValue(mu,-1.0,-0.5,0.5)*0.208;
	}
	else {
	  float mu = float(minziter);
	  mu = 0.55*10.0/(mu+10.0)+0.45*sqrt(minz);//sqrt(minz) adds a little gradient to the interior 
	  colour.x = getColourValue(mu,0.0,0.7,1.0)*0.302 + getColourValue(mu,0.0,0.4,0.7)*0.867 + getColourValue(mu,0.0,0.1,0.5);
	  colour.y = getColourValue(mu,0.0,0.7,1.0)*0.114 + getColourValue(mu,0.0,0.4,0.7)*0.282 + getColourValue(mu,0.0,0.1,0.5);
	  colour.z = getColourValue(mu,0.0,0.7,1.0)*0.208 + getColourValue(mu,0.0,0.4,0.7)*0.078 + getColourValue(mu,0.0,0.1,0.5);
	}
	
	
  outputF = vec4( colour.x, colour.y, colour.z, 1.0 );
}
