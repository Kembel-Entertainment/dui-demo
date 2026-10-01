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
vec4 demoRace(vec2 q,vec2 size,int a,int b,float t,bool live){
 int winner=a&3,seed=(a>>2)&255,selected=(a>>10)&3;bool racing=(b&512)!=0,finished=(b&1024)!=0;
 float u=racing?(live?clamp(t/(float(b&511)/20.0),0.0,1.0):1.0):(finished?1.0:0.0);
 float lane=size.y/4.0;int i=clamp(int(q.y/lane),0,3);float y=(float(i)+.5)*lane;
 vec3 c=mix(vec3(.025,.075,.085),vec3(.035,.14,.13),q.x/size.x);
 c+=float(i%2)*vec3(.015,.035,.025);
 c=dcPaint(c,dcRunner(i)*.42,dcLine(q,vec2(6,(float(i)+1.0)*lane),vec2(size.x-6,(float(i)+1.0)*lane),.5));
 float finishX=size.x-30.0;
 if(q.x>finishX&&q.x<finishX+12.0)c=mix(vec3(.95),vec3(.08,.13,.14),float((int(q.x/4.0)+int(q.y/4.0))%2));
 c=dcPaint(c,vec3(.6,.85,.78),dcLine(q,vec2(25,0),vec2(25,size.y),.5));
 float lead=i==winner?1.0:.81+float((i+seed)%3)*.033;
 float progress=clamp(u*lead+sin(u*10.0+float(i*3+seed))*.10*u*(1.0-u),0.0,1.0);
 float x=mix(31.0,finishX+4.0,progress),scale=min(lane*.26,8.0),gait=racing&&u<1.0?sin(t*21.0+float(i)*1.4):0.0;
 vec2 h=(q-vec2(x,y))/scale;
 // Ground shadow, galloping legs, warm coat, coloured saddle and jockey cap.
 c=dcPaint(c,vec3(.015,.025,.028),dcBox(h,vec2(-.2,1.0),vec2(1.9,.20),.15));
 vec3 coat=i%2==0?vec3(.63,.38,.20):vec3(.83,.66,.42);
 c=dcPaint(c,coat*.6,dcLine(h,vec2(-1.0,.0),vec2(-1.4-gait*.5,.95),.14));
 c=dcPaint(c,coat*.8,dcLine(h,vec2(.5,0),vec2(.7+gait*.6,.95),.14));
 c=dcPaint(c,coat,dcBox(h,vec2(-.35,-.15),vec2(1.1,.48),.42));
 c=dcPaint(c,coat,dcLine(h,vec2(.4,-.28),vec2(.8,-1.0),.30));
 c=dcPaint(c,coat*1.2,dcBox(h,vec2(1.05,-1.02),vec2(.58,.28),.18));
 c=dcPaint(c,coat*.45,dcLine(h,vec2(-1.35,-.3),vec2(-2.0,-.1+gait*.22),.15));
 c=dcPaint(c,dcRunner(i),dcBox(h,vec2(-.35,-.4),vec2(.65,.27),.12));
 c=dcPaint(c,vec3(.95,.90,.79),dcLine(h,vec2(-.5,-.62),vec2(-.25,-1.22),.15));
 c=dcPaint(c,dcRunner(i),dcCircle(h,vec2(-.15,-1.3),.3));
 c=dcPaint(c,vec3(.05),dcCircle(h,vec2(1.1,-1.05),.055));
 if(racing&&u<.99)for(int d=0;d<3;d++){
   vec2 puff=vec2(x-scale*(2.5+float(d)*.6),y+scale*.8);
   c=dcPaint(c,dcRunner(i)*.6,dcCircle(q,puff,scale*.13*(1.0+sin(t*12.0+float(d))))*.5);
 }
 c=dcPaint(c,dcRunner(i),dcNumber(q-vec2(11,y),i+1,max(.55,lane/18.0)));
 if(i==selected)c=dcPaint(c,dcRunner(i),dcLine(q,vec2(1,float(i)*lane+3),vec2(1,(float(i)+1.0)*lane-3),1.2));
 return vec4(c,1);
}
