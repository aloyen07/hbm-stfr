#version 120
uniform sampler2D sceneDepth;
uniform sampler3D boxMin;
uniform sampler3D boxMax;
uniform vec2 viewport;
uniform vec3 tint;
uniform float intensity;
uniform float radius;
uniform float time;
varying vec3 camera;
varying vec3 viewRay;

float cellHit(vec3 cell,vec3 origin,vec3 inverse,float near,float far) {
    if(any(lessThan(cell,vec3(0.0))) || any(greaterThan(cell,vec3(36.0)))) return far+1.0;
    float hit=far+1.0;
    for(int slot=0;slot<8;slot++) {
        vec3 uv=vec3((cell.xy+vec2(0.5))/37.0,(cell.z+float(slot)*37.0+0.5)/296.0);
        vec4 lo=texture3D(boxMin,uv);
        if(lo.a<0.5) break;
        vec3 hi=texture3D(boxMax,uv).xyz;
        vec3 a=(cell-vec3(18.5)+lo.xyz-origin)*inverse;
        vec3 b=(cell-vec3(18.5)+hi-origin)*inverse;
        vec3 entry=min(a,b),exit=max(a,b);
        float start=max(near,max(entry.x,max(entry.y,entry.z)));
        float end=min(exit.x,min(exit.y,exit.z));
        if(end>=start && start<=far) hit=min(hit,start);
    }
    return hit;
}

float viewLimit(vec3 direction,float near,float far) {
    vec3 grid=camera+direction*(near+0.001)+vec3(18.5);
    vec3 cell=floor(grid);
    vec3 safe=sign(direction)*max(abs(direction),vec3(0.00001));
    if(abs(safe.x)<0.000001) safe.x=0.00001;
    if(abs(safe.y)<0.000001) safe.y=0.00001;
    if(abs(safe.z)<0.000001) safe.z=0.00001;
    vec3 stepCell=sign(safe);
    vec3 next=(cell+max(stepCell,vec3(0.0))-grid)/safe;
    vec3 delta=abs(1.0/safe);
    float travelled=0.0;
    for(int i=0;i<128;i++) {
        if(near+travelled>=far) return far;
        float hit=cellHit(cell,camera,1.0/safe,near,far);
        if(hit<=far) return hit;
        if(next.x<next.y && next.x<next.z) { travelled=next.x;next.x+=delta.x;cell.x+=stepCell.x; }
        else if(next.y<next.z) { travelled=next.y;next.y+=delta.y;cell.y+=stepCell.y; }
        else { travelled=next.z;next.z+=delta.z;cell.z+=stepCell.z; }
    }
    return near+travelled;
}
// DDA visits cells; ray/AABB tests preserve the empty space around pipes and stairs.
float visible(vec3 p) {
    float lengthToSource=length(p);
    if(lengthToSource<0.1) return 1.0;
    vec3 direction=p/lengthToSource;
    vec3 safe=sign(direction)*max(abs(direction),vec3(0.00001));
    if(abs(safe.x)<0.000001) safe.x=0.00001;
    if(abs(safe.y)<0.000001) safe.y=0.00001;
    if(abs(safe.z)<0.000001) safe.z=0.00001;
    vec3 stepCell=sign(safe);
    vec3 cell=vec3(18.0);
    vec3 next=(cell+max(stepCell,vec3(0.0))-vec3(18.5))/safe;
    vec3 delta=abs(1.0/safe);
    float travelled=0.0;
    for(int i=0;i<64;i++) {
        if(travelled>=lengthToSource-0.05) return 1.0;
        if(cellHit(cell,vec3(0.0),1.0/safe,0.001,lengthToSource-0.05)<=lengthToSource-0.05) return 0.0;
        if(next.x<next.y && next.x<next.z) { travelled=next.x;next.x+=delta.x;cell.x+=stepCell.x; }
        else if(next.y<next.z) { travelled=next.y;next.y+=delta.y;cell.y+=stepCell.y; }
        else { travelled=next.z;next.z+=delta.z;cell.z+=stepCell.z; }
    }
    return 0.0;
}
void main() {
    vec3 direction=normalize(viewRay);
    float projection=dot(camera,direction);
    float discriminant=projection*projection-dot(camera,camera)+radius*radius;
    if(discriminant<=0.0) discard;
    float near=max(0.0,-projection-sqrt(discriminant));
    float far=-projection+sqrt(discriminant);
    vec2 screen=gl_FragCoord.xy/viewport;
    float depth=texture2D(sceneDepth,screen).r;
    vec4 scene=gl_ProjectionMatrixInverse*vec4(screen*2.0-1.0,depth*2.0-1.0,1.0);
    far=min(far,length(scene.xyz/scene.w));
    far=viewLimit(direction,near,far);
    if(far<=near) discard;
    float stride=(far-near)/24.0;
    float light=0.0;
    for(int i=0;i<24;i++) {
        float distance=near+(float(i)+0.5)*stride;
        vec3 p=camera+direction*distance;
        float distanceToCore=length(p);
        float density=exp(-distanceToCore*distanceToCore/(radius*radius*90.0/324.0))
                *(1.0-smoothstep(radius*2.0/3.0,radius,distanceToCore));
        light+=density*visible(p)*stride*0.022;
    }
    // Smooth emissive halo is integrated with the same occlusion, not a mesh sphere.
    float pulse=0.96+0.04*sin(time*0.037);
    gl_FragColor=vec4(tint*light*intensity*pulse,0.0);
}
