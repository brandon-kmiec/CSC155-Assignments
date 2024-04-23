#version 430

layout (location=0) in vec3 vertPosition;
layout (location=1) in vec2 texCoord;
layout (location = 2) in vec3 vertNormal;

uniform mat4 mv_matrix;

out vec2 tc;

out vec3 varyingNormal;
out vec3 varyingLightDir;
out vec3 varyingVertPos;

struct PositionalLight
{	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	vec3 position;
};
struct Material
{	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;
uniform mat4 norm_matrix;
uniform int toggleLight;


void main(void)
{
	varyingVertPos = (m_matrix * vec4(vertPosition,1.0)).xyz;
	varyingLightDir = light.position - varyingVertPos;
	varyingNormal = (norm_matrix * vec4(vertNormal,1.0)).xyz;
	
	gl_Position = p_matrix * mv_matrix * vec4(vertPosition,1.0);
	//gl_Position = p_matrix * v_matrix * m_matrix * vec4(vertPosition,1.0);
	
	tc = texCoord;
} 