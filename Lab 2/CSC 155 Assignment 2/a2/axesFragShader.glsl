#version 430

out vec4 c;
in vec4 color;

uniform mat4 mv_matrix;
uniform mat4 p_matrix;

void main(void)
{	c = color;
}
