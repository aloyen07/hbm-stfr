#version 120
uniform vec3 core;
varying vec3 camera;
varying vec3 viewRay;
void main() {
    gl_Position=vec4(gl_Vertex.xy,0.0,1.0);
    vec4 eye=gl_ProjectionMatrixInverse*vec4(gl_Vertex.xy,1.0,1.0);
    viewRay=(gl_ModelViewMatrixInverse*vec4(eye.xyz/eye.w,0.0)).xyz;
    camera=(gl_ModelViewMatrixInverse*vec4(0.0,0.0,0.0,1.0)).xyz-core;
}
