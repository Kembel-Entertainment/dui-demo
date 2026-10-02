// Consumer-owned vector drawing helpers. All coordinates are effect-local GUI pixels.
float dcMask(float d){float aa=max(fwidth(d),.01);return 1.0-smoothstep(-aa,aa,d);}
float dcCircle(vec2 p,vec2 centre,float radius){return dcMask(length(p-centre)-radius);}
float dcBox(vec2 p,vec2 centre,vec2 halfSize,float rounding){vec2 d=abs(p-centre)-halfSize+rounding;return dcMask(length(max(d,0.0))+min(max(d.x,d.y),0.0)-rounding);}
float dcLine(vec2 p,vec2 a,vec2 b,float width){vec2 pa=p-a,ba=b-a;return dcMask(length(pa-ba*clamp(dot(pa,ba)/dot(ba,ba),0.0,1.0))-width);}
vec3 dcPaint(vec3 base,vec3 colour,float ink){return mix(base,colour,clamp(ink,0.0,1.0));}
vec3 dcGold(float y){return mix(vec3(.49,.25,.055),vec3(1.0,.83,.39),.6+.4*cos(y*.07));}
bool dcDigit(vec2 p,int digit){ivec2 v=ivec2(floor(p));if(v.x<0||v.x>4||v.y<0||v.y>6)return false;
 const int rows[70]=int[70](14,17,17,17,17,17,14,4,6,4,4,4,4,14,14,17,16,8,4,2,31,15,16,16,14,16,16,15,8,12,10,9,31,8,8,31,1,1,15,16,16,15,14,1,1,15,17,17,14,31,16,8,4,2,2,2,14,17,17,14,17,17,14,14,17,17,30,16,16,14);
 return ((rows[clamp(digit,0,9)*7+v.y]>>v.x)&1)!=0;}
float dcNumber(vec2 p,int n,float unit){p/=unit;float w=n<10?5.0:11.0;p+=vec2(w*.5,3.5);return float(dcDigit(p,n<10?n:n/10)||(n>=10&&dcDigit(p-vec2(6,0),n%10)));}
vec3 dcRunner(int i){return i==0?vec3(.35,.95,.68):i==1?vec3(1.0,.75,.28):i==2?vec3(.35,.70,1.0):vec3(1.0,.40,.70);}
