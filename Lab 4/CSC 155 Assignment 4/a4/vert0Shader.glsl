#version 430

layout (location=0) in vec3 vertPosition;

uniform mat4 shadowMVP;

out vec4 vertColor;

void main(void)
{	gl_Position = shadowMVP * vec4(vertPosition,1.0);
	
	vertColor = vec4(1.0, 0.0, 0.0, 1.0);
}
