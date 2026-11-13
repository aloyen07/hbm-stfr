#version 120
uniform sampler2D lightBuffer;
uniform vec2 viewport;
void main() {
    gl_FragColor=vec4(texture2D(lightBuffer,gl_FragCoord.xy/viewport).rgb,0.0);
}
