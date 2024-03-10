#version 430

uniform float offsetX;
uniform float offsetY;
uniform int key1PressCount;
uniform int colorButtonCount;
uniform float scaleFactor;

out vec4 vertColor;

void main(void)
{ 
	if (gl_VertexID == 0) {	
		if (key1PressCount % 4 == 0) // Left
			gl_Position = vec4((0.25 + offsetX) * scaleFactor, (-0.25 + offsetY) * scaleFactor, 0.0, 1.0);
		else if (key1PressCount % 4 == 1) // Up
			gl_Position = vec4((-0.125 + offsetX) * scaleFactor, (-0.5 + offsetY) * scaleFactor, 0.0, 1.0);
		else if (key1PressCount % 4 == 2) // Right
			gl_Position = vec4((-0.25 + offsetX) * scaleFactor, (0.25 + offsetY) * scaleFactor, 0.0, 1.0);
		else // Down
			gl_Position = vec4((0.125 + offsetX) * scaleFactor, (0.5 + offsetY) * scaleFactor, 0.0, 1.0);
		
		if (colorButtonCount % 4 == 0) // Blue
			vertColor = vec4(0.0, 0.0, 1.0, 1.0);
		else if (colorButtonCount % 4 == 1) // Green
			vertColor = vec4(0.0, 1.0, 0.0, 1.0);
		else if (colorButtonCount % 4 == 2) // Orange
			vertColor = vec4(1.0, 0.5, 0.0, 1.0);
		else // Blue
			vertColor = vec4(0.0, 0.0, 1.0, 1.0);
	} // end if
	else if (gl_VertexID == 1) {
		if (key1PressCount % 4 == 0) // Left
			gl_Position = vec4((-0.5 + offsetX) * scaleFactor, (0.0 + offsetY) * scaleFactor, 0.0, 1.0);
		else if (key1PressCount % 4 == 1) // Up
			gl_Position = vec4((0.0 + offsetX) * scaleFactor, (0.75 + offsetY) * scaleFactor, 0.0, 1.0);
		else if (key1PressCount % 4 == 2) // Right
			gl_Position = vec4((0.5 + offsetX) * scaleFactor, (0.0 + offsetY) * scaleFactor, 0.0, 1.0);
		else // Down
			gl_Position = vec4((0.0 + offsetX) * scaleFactor, (-0.75 + offsetY) * scaleFactor, 0.0, 1.0);

		if (colorButtonCount % 4 == 0) // Blue
			vertColor = vec4(0.0, 0.0, 1.0, 1.0);
		else if (colorButtonCount % 4 == 1) // Green
			vertColor = vec4(0.0, 1.0, 0.0, 1.0);
		else if (colorButtonCount % 4 == 2) // Orange
			vertColor = vec4(1.0, 0.5, 0.0, 1.0);
		else // Green
			vertColor = vec4(0.0, 1.0, 0.0, 1.0);
	} // end else if
	else {
		if (key1PressCount % 4 == 0) // Left
			gl_Position = vec4((0.25 + offsetX) * scaleFactor, (0.25 + offsetY) * scaleFactor, 0.0, 1.0);
		else if (key1PressCount % 4 == 1) // Up
			gl_Position = vec4((0.125 + offsetX) * scaleFactor, (-0.5 + offsetY) * scaleFactor, 0.0, 1.0);
		else if (key1PressCount % 4 == 2) // Right
			gl_Position = vec4((-0.25 + offsetX) * scaleFactor, (-0.25 + offsetY) * scaleFactor, 0.0, 1.0);
		else // Down
			gl_Position = vec4((-0.125 + offsetX) * scaleFactor, (0.5 + offsetY) * scaleFactor, 0.0, 1.0);
			
		if (colorButtonCount % 4 == 0) // Blue
			vertColor = vec4(0.0, 0.0, 1.0, 1.0);
		else if (colorButtonCount % 4 == 1) // Green
			vertColor = vec4(0.0, 1.0, 0.0, 1.0);
		else if (colorButtonCount % 4 == 2) // Orange
			vertColor = vec4(1.0, 0.5, 0.0, 1.0);
		else // Orange
			vertColor = vec4(1.0, 0.5, 0.0, 1.0);
	} // end else
} // end main