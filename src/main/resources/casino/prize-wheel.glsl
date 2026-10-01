vec3 dcPrizeColour(int i){const vec3 colours[8]=vec3[8](vec3(.25,.19,.47),vec3(.20,.70,.75),vec3(.93,.35,.58),vec3(.20,.24,.42),vec3(.54,.37,.89),vec3(.15,.66,.53),vec3(1.0,.62,.22),vec3(1.0,.82,.34));return colours[i];}
vec4 demoPrizeWheel(vec2 q,vec2 size,int a,int b,float t,bool live){
 float tau=6.28318530718,step=tau/8.0,R=min(size.x,size.y)*.46;
 vec2 p=q-size*.5;float r=length(p),theta=atan(p.x,-p.y);
 int target=a&7,previous=(a>>3)&7;bool spin=(b&512)!=0;
 float u=spin&&live?clamp(t/(float(b&511)/20.0),0.0,1.0):1.0;
 float travel=5.0*tau+mod(float(previous-target)*step,tau),rotation=-float(previous)*step+travel*(1.0-pow(1.0-u,5.0));
 float angle=mod(theta-rotation+step*.5,tau),sector=floor(angle/step),local=mod(angle,step)-step*.5;
 int i=int(sector);const int prizes[8]=int[8](0,1,2,0,3,1,5,10);
 vec3 c=vec3(.055,.065,.15);float alpha=0;
 if(r<R+4.0){alpha=1;c=dcGold(q.y)*.4;if(r<R)c=dcPrizeColour(i)*(1.03-.30*r/R);}
 if(r<R*.92&&r>R*.27){
   c=dcPaint(c,vec3(1.0,.91,.63),dcMask((step*.49-abs(local))*r));
   vec2 label=vec2(sin(local)*r,R*.66-cos(local)*r);
   float unit=R*.027;
   c=dcPaint(c,vec3(.99,.98,.92),dcNumber(label,prizes[i],unit));
 }
 c=dcPaint(c,dcGold(q.y),dcMask(abs(r-R*.96)-R*.025));
 for(int peg=0;peg<24;peg++){
   float ang=float(peg)*tau/24.0;vec2 dotp=size*.5+vec2(sin(ang),-cos(ang))*R*.96;
   c=dcPaint(c,vec3(1.0,.95,.70),dcCircle(q,dotp,max(1.0,R*.014)));
 }
 c=dcPaint(c,vec3(.11,.13,.26),dcCircle(q,size*.5,R*.28));
 c=dcPaint(c,dcGold(q.y),dcMask(abs(r-R*.27)-1.3));
 float starR=R*(.12+.025*cos(theta*5.0));
 c=dcPaint(c,vec3(1.0,.84,.42),dcMask(r-starR));
 // Fixed top pointer and its gold cap; only the disc rotates.
 vec2 tip=q-vec2(size.x*.5,size.y*.5-R);
 if(tip.y>-8.0&&tip.y<7.0&&abs(tip.x)<(7.0-tip.y)*.52){alpha=1;c=vec3(1.0,.95,.73);}
 return vec4(c,alpha);
}
