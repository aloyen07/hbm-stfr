#version 120
varying vec3 lightViewPosition;
varying vec2 texcoord;
void main() {
    vec4 p=gl_ModelViewMatrix*gl_Vertex;
    lightViewPosition=p.xyz;
    texcoord=gl_MultiTexCoord0.xy;
    gl_Position=gl_ProjectionMatrix*p;
}
