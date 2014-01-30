// Fragment program
#version 150
#extension GL_ARB_gpu_shader_fp64 : enable

//#define PERIODICITY_CHECKING_THRESHHOLD 1E-16 //careful with this value; it can lead to inaccuracy (disabled)

uniform dvec2 center;
uniform float invzoom;
uniform int maxIteration;
uniform float transformParam;

uniform sampler2D palette;

in vec2 pos;

out vec4 outputF;

//float getColourValue(float mu, float s, float m, float e)
//{
//    if (mu<s) {
//        return 0.0;
//    } else if (mu<m) {
//        return (mu - s)/(m-s);
//    } else if (mu<e) {
//        return 1.0 - (mu - m)/(e-m);
//    }
//    return 0.0;
//}

void main( void ) {
	dvec2 c = center + dvec2(pos)*invzoom;
	dvec2 z = c;
	int iteration = 0;
	//vec3 colour = vec3(0.0,0.0,0.0);
	
	//for periodicity checking
	//int period = 8;
	//int periodCounter = 0;
	//dvec2 prevz = z;
	
	//bool foundPeriod = false;//for testing
	
	while (iteration<maxIteration && length(vec2(z.x,z.y)) < 3.0) {
		//periodicity checking (disabled)
		//if (periodCounter >= period){
		//	if (abs(z.x-prevz.x) < PERIODICITY_CHECKING_THRESHHOLD && abs(z.y-prevz.y) < PERIODICITY_CHECKING_THRESHHOLD){
		//		iteration = maxIteration;
		//		foundPeriod = true;
		//		break;
		//	}
		//	periodCounter = 0;
    //  period+=period;
		//	prevz = z;
		//}
		
		// do z = z^2 + c
		z = dvec2(z.x*z.x-z.y*z.y,2.0*z.x*z.y) + c;
		
		iteration++;
		//periodCounter++;
	}
	
	//colour = vec3(float(iteration)/maxIteration,0.0,0.0);
    
	/*if (foundPeriod){
	  colour = vec3(0.0,0.0,1.0-float(iteration)/maxIteration);
	  colour = vec3(1.0,1.0,1.0);
	}
	else*/
	if (iteration < maxIteration){
		//smooth colouring
		float mu = float(iteration+1) - log(log(length(vec2(z))))/0.6931471805599453; //log(2.0) = 0.6931471805599453
    mu = max(mu,0.0);
		//transform to between 0 and 1
		//mu/(mu+constant) goes to 1 as mu goes to infinity and to 0 as mu goes to 0.
		// This transformation is much better than a simple mu = mu/maxIteration because it 
		// is independent of maxIteration
		mu=mu/(mu+transformParam);
		
		ivec2 tsize = textureSize(palette,0);
		
		int tx, ty;
		ty = int(mu*tsize.y);
		tx = int((mu*tsize.y - ty)*tsize.x);
		
		//colour.x = getColourValue(mu,0.4,0.5,1.0);
		//colour.y = getColourValue(mu,0.3,0.5,0.7);
		//colour.z = getColourValue(mu,0.0,0.5,0.6);
		outputF = texelFetch(palette,ivec2(tx,ty),0);
	} else {
		outputF = vec4( 0.0, 0.0, 0.0, 1.0 );
	}
	
  //outputF = vec4( colour.x, colour.y, colour.z, 1.0 );
}
