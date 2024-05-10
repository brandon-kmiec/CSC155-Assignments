#version 430

in vec2 tc;

in vec3 varyingNormal;
in vec3 varyingLightDir;
in vec3 varyingVertPos;
in vec3 varyingHalfVec;

in vec4 shadow_coord;

out vec4 color;

uniform mat4 mv_matrix;
//uniform mat4 p_matrix;

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
uniform mat4 shadowMVP;
uniform int toggleLight;

layout (binding=1) uniform sampler2DShadow shadowTex;

layout (binding=0) uniform sampler2D samp;

void main(void)
{
	vec4 textColor = texture(samp, tc);
	
	if(toggleLight == 1) {
		// normalize the light, normal, and view vectors:
		vec3 L = normalize(varyingLightDir);
		vec3 N = normalize(varyingNormal);
		vec3 V = normalize(-v_matrix[3].xyz - varyingVertPos);
		vec3 H = normalize(varyingHalfVec);
		
		float notInShadow = textureProj(shadowTex, shadow_coord);
		
		//fragColor = globalAmbient * material.ambient + light.ambient * material.ambient;
	
		// compute light reflection vector, with respect N:
		vec3 R = normalize(reflect(-L, N));
	
		// get the angle between the light and surface normal:
		float cosTheta = dot(L,N);
	
		// angle between the view vector and reflected light:
		float cosPhi = dot(V,R);

		// compute ADS contributions (per pixel):
		vec3 ambient = ((globalAmbient * material.ambient) + (light.ambient * material.ambient)).xyz;
		vec3 diffuse = light.diffuse.xyz * material.diffuse.xyz * max(cosTheta,0.0);
		vec3 specular = light.specular.xyz * material.specular.xyz * pow(max(cosPhi,0.0), material.shininess);
	
		//color = vec4(varyingNormal, 1.0);
		//color = texture(samp, tc);
		color = min((textColor * vec4((ambient + diffuse), 1.0) + vec4(specular, 1.0)), vec4(1,1,1,1));
		//color = texture(samp, tc) * vec4((ambient + diffuse + specular), 1.0);
		//color = vec4((ambient + diffuse + specular), 1.0);
		//fragColor = texture(samp, tc) * vec4((ambient + diffuse), 1.0) + vec4((
		
		if (notInShadow == 1.0) {	
			color += light.diffuse * material.diffuse * max(dot(L,N),0.0)
					+ light.specular * material.specular
					* pow(max(dot(H,N),0.0),material.shininess*3.0);
		} // end if
	} // end if
	else if(toggleLight == 0) {
		color = textColor * globalAmbient;
	} // end else if
}
