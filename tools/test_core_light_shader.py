"""Software OpenGL smoke test of the actual shipped GLSL (no game or window required)."""
import ctypes as C
import os
from pathlib import Path
os.environ.setdefault('LIBGL_ALWAYS_SOFTWARE', '1')
os.environ.setdefault('MESA_SHADER_CACHE_DISABLE', 'true')
E=C.CDLL('libEGL.so.1'); G=C.CDLL('libGL.so.1')
def fn(lib,name,result,args):
    f=getattr(lib,name);f.restype=result;f.argtypes=args;return f
P=C.c_void_p; I=C.c_int; U=C.c_uint; F=C.c_float
getproc=fn(E,'eglGetProcAddress',P,[C.c_char_p])
platform=C.CFUNCTYPE(P,U,P,C.POINTER(I))(getproc(b'eglGetPlatformDisplayEXT'))
display=platform(0x31DD,None,None)
assert fn(E,'eglInitialize',U,[P,C.POINTER(I),C.POINTER(I)])(display,None,None), 'EGL initialization failed'
assert fn(E,'eglBindAPI',U,[U])(0x30A2)
attrs=(I*11)(0x3033,1,0x3040,8,0x3024,8,0x3023,8,0x3022,8,0x3038)
config=P();count=I()
assert fn(E,'eglChooseConfig',U,[P,C.POINTER(I),C.POINTER(P),I,C.POINTER(I)])(display,attrs,C.byref(config),1,C.byref(count)) and count.value
surface=fn(E,'eglCreatePbufferSurface',P,[P,P,C.POINTER(I)])(display,config,(I*5)(0x3057,32,0x3056,32,0x3038))
context=fn(E,'eglCreateContext',P,[P,P,P,C.POINTER(I)])(display,config,None,(I*1)(0x3038))
assert fn(E,'eglMakeCurrent',U,[P,P,P,P])(display,surface,surface,context)
create=fn(G,'glCreateShader',U,[U]);source=fn(G,'glShaderSource',None,[U,I,C.POINTER(C.c_char_p),P]);compile_=fn(G,'glCompileShader',None,[U]);get=fn(G,'glGetShaderiv',None,[U,U,C.POINTER(I)])
shaders=[]
for suffix,kind in [('vert',0x8B31),('frag',0x8B30)]:
    shader=create(kind);data=Path(f'src/main/resources/assets/hbmstfr/shaders/core_light.{suffix}').read_bytes()
    source(shader,1,C.byref(C.c_char_p(data)),None);compile_(shader);status=I();get(shader,0x8B81,C.byref(status))
    log=C.create_string_buffer(8192);fn(G,'glGetShaderInfoLog',None,[U,I,P,P])(shader,8192,None,log)
    assert status.value,log.value.decode();shaders.append(shader)
program=fn(G,'glCreateProgram',U,[])()
for shader in shaders:fn(G,'glAttachShader',None,[U,U])(program,shader)
fn(G,'glLinkProgram',None,[U])(program)
status=I();fn(G,'glGetProgramiv',None,[U,U,C.POINTER(I)])(program,0x8B82,C.byref(status));assert status.value
fn(G,'glUseProgram',None,[U])(program)
loc=fn(G,'glGetUniformLocation',I,[U,C.c_char_p])
def ui(name,value):fn(G,'glUniform1i',None,[I,I])(loc(program,name.encode()),value)
def uf(name,value):fn(G,'glUniform1f',None,[I,F])(loc(program,name.encode()),value)
def v3(name,x,y,z):fn(G,'glUniform3f',None,[I,F,F,F])(loc(program,name.encode()),x,y,z)
for name in ['inverseProjection','inverseView']:
    matrix=(F*16)(1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1)
    if name=="inverseProjection":matrix[14]=-2  # Surface is in front of the camera, never coplanar with it.
    fn(G,'glUniformMatrix4fv',None,[I,I,U,C.POINTER(F)])(loc(program,name.encode()),1,0,matrix)
active=fn(G,'glActiveTexture',None,[U]);bind=fn(G,'glBindTexture',None,[U,U]);image=fn(G,'glTexImage2D',None,[U,I,I,I,I,I,U,U,P]);param=fn(G,'glTexParameteri',None,[U,U,I])
for unit in [0,1,2,3]:
    active(0x84C0+unit);tex=U();fn(G,'glGenTextures',None,[I,C.POINTER(U)])(1,C.byref(tex));target=0x8513 if unit>=2 else 0x0DE1;bind(target,tex.value)
    for name in [0x2801,0x2800]:param(target,name,0x2600)
    if unit==0:image(target,0,0x1908,1,1,0,0x1908,0x1401,(C.c_ubyte*4)(25,25,25,255))
    if unit==1:image(target,0,0x1902,1,1,0,0x1902,0x1406,(F*1)(.5))
    if unit==3:
        for face in range(6):image(0x8515+face,0,0x1908,1,1,0,0x1908,0x1401,(C.c_ubyte*4)(255,255,0,255))
ui('entityShadow',3);ui('entityReady',0);uf('shadowSize',32);uf('entitySize',128);
ui('scene',0);ui('depth',1);ui('shadow',2);ui('drawLight',1);ui('drawStar',0);ui('shadowReady',1);ui('measure',0)
v3('starColor',1,1,1);uf('starPower',8);v3('lightPosition',0,0,-1);v3('lightColor',1,.88,.68);uf('radius',10);uf('intensity',1);uf('exposure',1);uf('lightExposure',1)
fn(G,'glUniform2f',None,[I,F,F])(loc(program,b'screenSize'),32,32)
fn(G,'glUniform2f',None,[I,F,F])(loc(program,b'starPosition'),.5,.5)
uf('starSize',.15);uf('starVisibility',0)
fn(G,'glViewport',None,[I,I,I,I])(0,0,32,32)
def render(cube_depth=None,measure=0):
    active(0x84C2)
    if cube_depth is not None:
        for face in range(6):image(0x8515+face,0,0x1908,1,1,0,0x1908,0x1401,(C.c_ubyte*4)(cube_depth,cube_depth,0,255))
    ui('measure',measure)
    fn(G,'glBegin',None,[U])(7)
    for x,y,u,v in [(-1,-1,0,0),(1,-1,1,0),(1,1,1,1),(-1,1,0,1)]:
        fn(G,'glTexCoord2f',None,[F,F])(u,v);fn(G,'glVertex2f',None,[F,F])(x,y)
    fn(G,'glEnd',None,[])()
    pixel=(C.c_ubyte*4)();fn(G,'glReadPixels',None,[I,I,I,I,U,U,P])(16,16,1,1,0x1908,0x1401,pixel)
    return tuple(pixel)
blocked=render(0);clear=render(255);energy=render(255,1)
assert blocked[0]<35,('Opaque wall leaked light',blocked)
assert clear[0]>150,('Unblocked surface not illuminated',clear)
assert energy[0]>100,('Surface light missing from exposure meter',energy)
uf('exposure',.5)
dim=render(0)
assert 9<=dim[0]<=15,('Exposure did not dim external light',dim)
uf('exposure',1);ui('drawLight',0);ui('drawStar',1)
fn(G,'glUniform2f',None,[I,F,F])(loc(program,b'starPosition'),.5,.5)
fn(G,'glUniform2f',None,[I,F,F])(loc(program,b'screenSize'),32,32)
uf('starSize',.15);uf('starVisibility',1)
star=render(0)
uf('exposure',.25);uf('lightExposure',.25)
star_dimmed_world=render(0)
assert star_dimmed_world[0]>200,'Exposure incorrectly dimmed the star'
uf('exposure',1);uf('lightExposure',1)
uf('starVisibility',0)
hidden=render(0)
assert star[0]>200 and hidden[0]<35,('Star did not respect visibility',star,hidden)
# Off-screen or occluded stars must contribute no exposure, regardless of HUD state.
uf('starVisibility',1)
fn(G,'glUniform2f',None,[I,F,F])(loc(program,b'starPosition'),1.01,.5)
assert render(0,1)[0]==0,'An off-screen star darkened the environment'
fn(G,'glUniform2f',None,[I,F,F])(loc(program,b'starPosition'),.5,.5)
uf('starVisibility',0)
assert render(0,1)[0]==0,'An occluded star darkened the environment'
uf('starVisibility',1);uf('starPower',3.5);v3('starColor',1,.25,.15)
warm=render(0)
assert warm[0]>warm[1]>warm[2],'Warm core star lost its temperature tint'
uf('starPower',8);v3('starColor',1,1,1)
# Dynamic model shadow map is independently combined with terrain shadows.
ui('drawStar',0);ui('drawLight',1);ui('entityReady',1)
active(0x84C3)
for face in range(6):image(0x8515+face,0,0x1908,1,1,0,0x1908,0x1401,(C.c_ubyte*4)(0,0,0,255))
assert render(255)[0]<35,'Entity map failed to shadow terrain'
ui('entityReady',0)
# A radial depth map of an exact plane must not produce dark checkerboard tiles.
import math
active(0x84C2)
for face in range(6):
    data=[]
    for y in range(32):
        for x in range(32):
            u=2*(x+.5)/32-1;v=2*(y+.5)/32-1
            d=[(1,-v,-u),(-1,-v,u),(u,1,v),(u,-1,-v),(u,-v,1),(-u,-v,-1)][face]
            dz=d[2]/math.sqrt(sum(c*c for c in d))
            distance=min(10,-1/dz) if dz<0 else 10
            packed=round(distance/10*65535)
            data.extend([packed>>8,packed&255,0,255])
    image(0x8515+face,0,0x1908,32,32,0,0x1908,0x1401,(C.c_ubyte*len(data))(*data))
render()
pixels=(C.c_ubyte*(32*32*4))()
fn(G,'glReadPixels',None,[I,I,I,I,U,U,P])(0,0,32,32,0x1908,0x1401,pixels)
assert min(pixels[i*4] for i in range(32*32))>150,'Planar shadow self-occlusion/checkerboard'
# Compile and exercise the actual entity-model radial depth shader as well.
entity_program=fn(G,'glCreateProgram',U,[])()
for suffix,kind in [('vert',0x8B31),('frag',0x8B30)]:
    shader=create(kind);data=Path(f'src/main/resources/assets/hbmstfr/shaders/entity_shadow.{suffix}').read_bytes()
    source(shader,1,C.byref(C.c_char_p(data)),None);compile_(shader);status=I();get(shader,0x8B81,C.byref(status))
    log=C.create_string_buffer(8192);fn(G,'glGetShaderInfoLog',None,[U,I,P,P])(shader,8192,None,log)
    assert status.value,log.value.decode()
    fn(G,'glAttachShader',None,[U,U])(entity_program,shader)
fn(G,'glLinkProgram',None,[U])(entity_program)
status=I();fn(G,'glGetProgramiv',None,[U,U,C.POINTER(I)])(entity_program,0x8B82,C.byref(status));assert status.value
fn(G,'glUseProgram',None,[U])(entity_program)
fn(G,'glUniform1i',None,[I,I])(loc(entity_program,b'skin'),0)
fn(G,'glUniform1f',None,[I,F])(loc(entity_program,b'radius'),10)
fn(G,'glClearColor',None,[F,F,F,F])(0,0,0,1)
def triangle():
    fn(G,'glClear',None,[U])(0x4000)
    fn(G,'glBegin',None,[U])(4)
    for x,y in [(-.8,-.8),(.8,-.8),(0,.8)]:
        fn(G,'glTexCoord2f',None,[F,F])(.5,.5)
        fn(G,'glVertex3f',None,[F,F,F])(x,y,.5)
    fn(G,'glEnd',None,[])()
    p=(C.c_ubyte*4)();fn(G,'glReadPixels',None,[I,I,I,I,U,U,P])(16,16,1,1,0x1908,0x1401,p)
    return tuple(p)
model=triangle();radial=(model[0]*256+model[1])/65535
assert .045<radial<.06,('Entity radial depth encoding incorrect',model,radial)
active(0x84C0);image(0x0DE1,0,0x1908,1,1,0,0x1908,0x1401,(C.c_ubyte*4)(255,255,255,0))
assert triangle()[:2]==(0,0),'Transparent skin pixels cast a solid shadow'
print('GLSL: lighting, terrain/entity shadows, planar continuity, alpha-cutout and exposure: PASS',blocked,clear,energy)
fn(E,'eglMakeCurrent',U,[P,P,P,P])(display,None,None,None)
fn(E,'eglDestroyContext',U,[P,P])(display,context)
fn(E,'eglDestroySurface',U,[P,P])(display,surface)
fn(E,'eglTerminate',U,[P])(display)
