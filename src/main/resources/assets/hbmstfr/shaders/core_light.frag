#version 120
uniform sampler2D scene,depth;
uniform samplerCube shadow,entityShadow;
uniform float shadowSize,entitySize;
uniform int entityReady;
uniform mat4 inverseProjection,inverseView;
uniform vec3 lightPosition,lightColor,starColor;
uniform float starPower;
uniform vec2 starPosition,screenSize;
uniform float radius,intensity,starVisibility,starSize,exposure,lightExposure;
uniform int drawLight,drawStar,measure,shadowReady;
varying vec2 uv;
vec3 position(vec2 p,float z) {
    vec4 eye=inverseProjection*vec4(p*2.0-1.0,z*2.0-1.0,1.0);
    return (inverseView*vec4(eye.xyz/eye.w,1.0)).xyz;
}
// Match the exact rays used to build each cubemap texel. This removes planar
// self-shadow grids caused by comparing different directions at low resolution.
vec3 texelRay(vec3 d,float size) {
    vec3 a=abs(d);vec2 t;float face;
    if(a.x>=a.y && a.x>=a.z) {
        face=d.x>0.0?0.0:1.0;t=vec2(d.x>0.0?-d.z:d.z,-d.y)/a.x;
    } else if(a.y>=a.z) {
        face=d.y>0.0?2.0:3.0;t=vec2(d.x,d.y>0.0?d.z:-d.z)/a.y;
    } else {
        face=d.z>0.0?4.0:5.0;t=vec2(d.z>0.0?d.x:-d.x,-d.y)/a.z;
    }
    t=(floor(clamp(t*0.5+0.5,0.0,0.999999)*size)+0.5)/size*2.0-1.0;
    if(face<0.5)return normalize(vec3(1.0,-t.y,-t.x));
    if(face<1.5)return normalize(vec3(-1.0,-t.y,t.x));
    if(face<2.5)return normalize(vec3(t.x,1.0,t.y));
    if(face<3.5)return normalize(vec3(t.x,-1.0,-t.y));
    if(face<4.5)return normalize(vec3(t.x,-t.y,1.0));
    return normalize(vec3(-t.x,-t.y,-1.0));
}
float sampleShadow(samplerCube map,vec3 delta,vec3 normal,float size) {
    vec3 d=normalize(delta);
    vec3 tangent=normalize(cross(d,abs(d.y)<0.9?vec3(0,1,0):vec3(1,0,0)));
    vec3 bitangent=cross(d,tangent);
    float sum=0.0;
    for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++) {
        vec3 ray=texelRay(d+(tangent*float(x)+bitangent*float(y))*1.4/size,size);
        vec2 depthRG=textureCube(map,ray).rg;
        float limit=dot(depthRG,vec2(65280.0,255.0))/65535.0*radius;
        float denominator=dot(ray,normal);
        float reference=abs(denominator)>0.02 ? dot(delta,normal)/denominator : length(delta);
        if(reference<=0.0)reference=length(delta);
        sum+=1.0-smoothstep(limit+0.04,limit+0.12,reference);
    }
    return sum/9.0;
}
float visible(vec3 delta,vec3 normal) {
    if(shadowReady==0)return 0.0;
    float terrain=sampleShadow(shadow,delta,normal,shadowSize);
    return entityReady==1 ? terrain*sampleShadow(entityShadow,delta,normal,entitySize) : terrain;
}
vec3 surfaceNormal(vec3 p) {
    vec2 stepUV=1.0/screenSize;
    vec3 r=position(uv+vec2(stepUV.x,0),texture2D(depth,uv+vec2(stepUV.x,0)).r)-p;
    vec3 l=p-position(uv-vec2(stepUV.x,0),texture2D(depth,uv-vec2(stepUV.x,0)).r);
    vec3 u=position(uv+vec2(0,stepUV.y),texture2D(depth,uv+vec2(0,stepUV.y)).r)-p;
    vec3 b=p-position(uv-vec2(0,stepUV.y),texture2D(depth,uv-vec2(0,stepUV.y)).r);
    vec3 n=normalize(cross(dot(r,r)<dot(l,l)?r:l,dot(u,u)<dot(b,b)?u:b));
    // Voxel faces are planar and axis-aligned; do not amplify depth quantization.
    vec3 a=abs(n);
    if(a.x>0.995)n=vec3(sign(n.x),0,0);
    else if(a.y>0.995)n=vec3(0,sign(n.y),0);
    else if(a.z>0.995)n=vec3(0,0,sign(n.z));
    return n;
}
void main() {
    float z=texture2D(depth,uv).r;
    vec3 radiance=vec3(0.0),coreRadiance=vec3(0.0);
    if(drawLight==1 && z<0.999999) {
        vec3 p=position(uv,z), delta=p-lightPosition;
        vec3 n=surfaceNormal(p);
        // Orient the reconstructed surface towards the camera.
        vec3 eye=(inverseView*vec4(0.0,0.0,0.0,1.0)).xyz;
        if(dot(n,eye-p)<0.0)n=-n;
        float attenuation=pow(max(0.0,1.0-length(delta)/radius),2.0);
        float diffuse=max(0.0,dot(n,-normalize(delta)));
        radiance+=lightColor*intensity*2.5*attenuation*diffuse*visible(delta,n);
    }
    if(drawStar==1 && starVisibility>0.0 && starPosition.x>=0.0 && starPosition.x<=1.0 && starPosition.y>=0.0 && starPosition.y<=1.0) {
        vec2 q=(uv-starPosition)*vec2(screenSize.x/screenSize.y,1.0);
        // Conservative pixel footprint keeps a tiny, extremely bright core in the meter.
        vec2 sampleQ=measure==1 ? max(abs(q)-vec2(screenSize.x/screenSize.y/16.0,1.0/9.0)*0.5,vec2(0.0)) : q;
        float r=length(sampleQ)/max(starSize,0.0001);
        float core=exp(-r*r*10.0)*starPower;
        float halo=0.14*exp(-r*0.45);
        float rays=0.12*exp(-min(abs(q.x),abs(q.y))*800.0)*exp(-r*(0.35/3.0));
        coreRadiance+=(starColor*core+lightColor*(halo+rays))*starVisibility*intensity;
    }
    if(measure==1) {
        float energy=dot(radiance+coreRadiance,vec3(0.2126,0.7152,0.0722));
        gl_FragColor=vec4(vec3(min(1.0,energy)),1.0);
    } else {
        vec3 base=texture2D(scene,uv).rgb;
        gl_FragColor=vec4(clamp(base*exposure+(1.0-exp(-radiance*lightExposure))+(1.0-exp(-coreRadiance)),0.0,1.0),1.0);
    }
}
