from pathlib import Path
import subprocess,struct
import moderngl
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
JAVA=Path.home()/'.codex/android-deckball-tools/jdk/bin'

def compile_scene() -> Path:
    classes=ROOT/'build/preview3d';classes.mkdir(parents=True,exist_ok=True)
    sources=[p for p in (ROOT/'src/com/deckball').glob('*.java') if 'import android.' not in p.read_text(encoding='utf-8-sig')]
    sources.append(ROOT/'tools/preview/com/deckball/SceneDump.java')
    sources.append(ROOT/'src/com/deckball/MatchOverlay.java')
    sources.append(ROOT/'tools/preview/com/deckball/OverlayDump.java')
    sources += list((ROOT/'tools/preview/android').rglob('*.java'))
    subprocess.run([str(JAVA/'javac.exe'),'-encoding','UTF-8','-d',str(classes),*map(str,sources)],check=True)
    subprocess.run([str(JAVA/'java.exe'),'-cp',str(classes),'com.deckball.SceneDump',str(classes/'scene.bin'),str(classes/'scene.vert'),str(classes/'scene.frag')],check=True)
    subprocess.run([str(JAVA/'java.exe'),'-cp',str(classes),'com.deckball.OverlayDump',str(classes/'overlay.png')],check=True)
    return classes

def shader(path: Path, vertex: bool) -> str:
    text=path.read_text(encoding='utf-8').replace('precision mediump float;','')
    text=text.replace('attribute','in').replace('varying','out' if vertex else 'in')
    if not vertex:text='out vec4 fragColor;\n'+text.replace('gl_FragColor','fragColor')
    return '#version 330\n'+text

def render() -> None:
    classes=compile_scene();raw=(classes/'scene.bin').read_bytes()
    ctx=moderngl.create_standalone_context()
    program=ctx.program(vertex_shader=shader(classes/'scene.vert',True),fragment_shader=shader(classes/'scene.frag',False))
    program['uCamera'].write(np.frombuffer(raw[:64],dtype='>f4').astype('f4').tobytes())
    target=ctx.simple_framebuffer((1600,900),components=3);target.use();ctx.enable(moderngl.DEPTH_TEST)
    target.clear(.64,.77,.83,1,depth=1);offset=64
    for _ in range(2):
        count=struct.unpack('>i',raw[offset:offset+4])[0];offset+=4
        values=np.frombuffer(raw[offset:offset+count*4],dtype='>f4').astype('f4');offset+=count*4
        buffer=ctx.buffer(values.tobytes());vao=ctx.vertex_array(program,[(buffer,'3f 3f 3f','aPosition','aNormal','aColor')]);vao.render()
    output=ROOT/'previews/field-3d-2026.png'
    image=Image.frombytes('RGB',(1600,900),target.read(components=3)).transpose(Image.Transpose.FLIP_TOP_BOTTOM).convert('RGBA')
    Image.alpha_composite(image,Image.open(classes/'overlay.png')).convert('RGB').save(output)
    print(str(output));print(ctx.info['GL_RENDERER'])

if __name__=='__main__':render()
