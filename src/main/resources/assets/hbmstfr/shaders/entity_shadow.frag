#version 120
uniform sampler2D skin;
uniform float radius;
varying vec3 lightViewPosition;
varying vec2 texcoord;
void main() {
    if(texture2D(skin,texcoord).a<0.1)discard;
    float value=floor(clamp(length(lightViewPosition)/radius,0.0,1.0)*65535.0+0.5);
    gl_FragColor=vec4(floor(value/256.0)/255.0,mod(value,256.0)/255.0,0.0,1.0);
}
