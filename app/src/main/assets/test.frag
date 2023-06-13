uniform sampler2D therender;

uniform vec4 coloradd;
attribute float blah;

uniform float moo;

varying float bullshitness;

@import misc.rlsl

uniform float huerot;


///////// here here!
void shaspect_huerotate(inout vec4 color){
    const vec4  kRGBToYPrime = vec4 (0.299, 0.587, 0.114, 0.0);
    const vec4  kRGBToI     = vec4 (0.596, -0.275, -0.321, 0.0);
    const vec4  kRGBToQ     = vec4 (0.212, -0.523, 0.311, 0.0);

    const vec4  kYIQToR   = vec4 (1.0, 0.956, 0.621, 0.0);
    const vec4  kYIQToG   = vec4 (1.0, -0.272, -0.647, 0.0);
    const vec4  kYIQToB   = vec4 (1.0, -1.107, 1.704, 0.0);

    // Sample the input pixel

    // Convert to YIQ
    float   YPrime  = dot (color, kRGBToYPrime);
    float   I      = dot (color, kRGBToI);
    float   Q      = dot (color, kRGBToQ);

    // Calculate the hue and chroma
    float   hue     = atan (Q, I);
    float   chroma  = sqrt (I * I + Q * Q);

    // Make the user's adjustments
    hue += huerot;

    // Convert back to YIQ
    Q = chroma * sin (hue);
    I = chroma * cos (hue);

    // Convert back to RGB
    vec4    yIQ   = vec4 (YPrime, I, Q, 0.0);
    color.r = dot (yIQ, kYIQToR);
    color.g = dot (yIQ, kYIQToG);
    color.b = dot (yIQ, kYIQToB);

    // Save the result
}

void main(void)
{
   // gl_FragColor = gl_Color;
   //gl_FragColor = texture2D(therender,gl_TexCoord[0].st) * gl_Color;
   // we'll try this first
   
   @stream vec4 finalcolor = vec4(0.5 + moo + coloradd.x, 0.5 + coloradd.y, 0.5 + blah + coloradd.z,1);
   
   shaspect_huerotate(finalcolor);
   
   gl_FragColor = finalcolor;
   
}
