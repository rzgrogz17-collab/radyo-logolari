#ifdef GL_ES
precision mediump float;
#endif

uniform vec2 u_resolution;
uniform float u_time;
uniform float u_dial_diameter;
uniform vec2 u_center;
uniform float u_alpha;
uniform vec2 u_pos;

void main() {
    vec2 p = vec2(gl_FragCoord.x-u_resolution.x * u_center.x - u_pos.x, gl_FragCoord.y-u_resolution.y * u_center.y - u_pos.y)/u_dial_diameter;

    float tau = 3.1415926535*2.0;
    float a = atan(p.x,p.y);
    float r = length(p);
    vec2 uv = vec2(a/tau,r);

	// Misty white: color is no longer a rotating RGB rainbow (red/purple/
	// green/yellow) - it's now a fixed, soft/misty white tone. The ring's
	// shape (beamWidth) and its breathing/wave animation (width modulated
	// by u_time) are left completely untouched - only the color is fixed.
	vec3 horColour = vec3(0.961, 0.973, 0.988);

	uv = (2.0 * uv) - 1.0;
	float beamWidth = (0.7+0.01*cos(uv.x*10.0*tau*0.15*clamp(floor(5.0 + 10.0*cos(u_time)), 0.0, 10.0))) * abs(1.0 / (30.0 * uv.y));

	vec4 color = vec4(0,0,0,0);
	color = mix(color, vec4(horColour, 1), beamWidth * u_alpha);
	gl_FragColor = color;
}