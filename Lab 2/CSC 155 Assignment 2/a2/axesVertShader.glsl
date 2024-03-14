#version 430

layout (location=0) in vec3 position;

uniform mat4 mv_matrix;
uniform mat4 p_matrix;

out vec4 color;

void main(void)
{

	gl_Position = p_matrix * mv_matrix * vec4(position, 1.0);
	
	if(gl_VertexID == 0 || gl_VertexID == 1)
		color = vec4(1.0, 0.0, 0.0, 1.0);
	else if(gl_VertexID == 2 || gl_VertexID == 3)
		color = vec4(0.0, 1.0, 0.0, 1.0);
	else if(gl_VertexID == 4 || gl_VertexID == 5)
		color = vec4(0.0, 0.0, 1.0, 1.0);
} 
