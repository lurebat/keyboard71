@import prelude.rlsl

@fragheader

@libimport

@varpoint

uniform vec4 _r_color;
varying vec4 _vr_colorout;

void main(void)
{
   
   @fragvarpoint
   
   @stream vec4 finalcolor = _vr_colorout;
   
   @splicepoint
   
   @endstream finalcolor
   
   gl_FragColor = finalcolor;
   //gl_FragColor = vec4(1,0,0,1);
   
}
