// Vertex program
#version 150

uniform mat4 matrix;
uniform mat4 fracPosMatrix;

in vec3 vertex;

out vec2 pos;

void main() {
	vec4 transformedPos = matrix*vec4(vertex, 1.0);
	gl_Position = transformedPos;
	transformedPos = fracPosMatrix*transformedPos;
  pos = transformedPos.xy;
}
