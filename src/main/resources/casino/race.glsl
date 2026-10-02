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
