@import prelude.rlsl

@vertheader

@varpoint

@libimport

uniform mat4 _r_mm; // roen model matrix
uniform mat4 _r_pm; // roen projection matrix
uniform mat4 _r_vm; // roen view matrix
uniform vec4 _r_color;

varying vec4 _vr_colorout;

attribute vec3 _vertpos;
attribute vec3 _vertmid;


void main(void) 
{

    @vertvarpoint

    @stream vec4 vertcolor = _r_color;

    @stream vec4 vertpos = vec4(_vertpos, 1.0);
    @stream vec3 vertmid = _vertmid;
    
    @splicepoint <50
    
    @endstream vertpos
    
    @stream vec4 mmpos = _r_mm * vertpos;
    
    @splicepoint >=50 <100
    
    @endstream mmpos
    @endstream vertcolor
    @stream vec4 vmpos = _r_vm * mmpos;
    
    @splicepoint >=100 <150
    
    @endstream vmpos;
    
    @stream vec4 pmpos = _r_pm * vmpos;
    gl_Position = pmpos;
    
    @splicepoint >=150
    
    _vr_colorout = vertcolor;
    
}
