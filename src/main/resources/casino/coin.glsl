vec4 demoCoin(vec2 q,vec2 size,int a,int b,float t,bool live){
 int face=a&1;bool flipping=(b&512)!=0;
 float u=flipping&&live?clamp(t/(float(b&511)/20.0),0.0,1.0):1.0;
 float R=min(size.x,size.y)*.36,phase=(1.0-pow(1.0-u,3.0))*6.2831853*5.0+float(face)*3.14159265;
 float squash=max(.055,abs(cos(phase))),lift=flipping?sin(u*3.14159265)*R*.34:0.0;
 vec2 centre=size*.5-vec2(0,lift),p=q-centre;
 vec3 c=vec3(0);float alpha=0.0;
 float shadow=dcBox(q,size*.5+vec2(0,R*1.02),vec2(R*(.62-.20*sin(u*3.14159265)),R*.07),R*.07);
 if(shadow>0.01){c=vec3(.13,.11,.10);alpha=shadow*.7;}
 vec2 local=vec2(p.x/squash,p.y);float r=length(local);
 if(r<R+3.0){alpha=1.0;c=dcGold(q.y)*(.70+.30*squash);}
 if(r<R){c=dcGold(q.y)*(.92+.10*cos(local.x*.08));
   c=dcPaint(c,vec3(.47,.26,.08),dcMask(abs(r-R*.88)-R*.018));
   for(int i=0;i<24;i++){float angle=float(i)*6.2831853/24.0;
     vec2 bead=vec2(sin(angle),cos(angle))*R*.76;
     c=dcPaint(c,vec3(1.0,.91,.60),dcCircle(local,bead,R*.017));}
   bool moon=cos(phase)<0.0;
   if(moon){
     c=dcPaint(c,vec3(.49,.25,.055),dcCircle(local,vec2(0),R*.39));
     c=dcPaint(c,dcGold(q.y),dcCircle(local,vec2(R*.19,-R*.12),R*.34));
     c=dcPaint(c,vec3(.47,.24,.04),dcCircle(local,vec2(R*.33,R*.19),R*.045));
   }else{
     c=dcPaint(c,vec3(.47,.23,.045),dcCircle(local,vec2(0),R*.25));
     c=dcPaint(c,vec3(1.0,.90,.56),dcCircle(local,vec2(-R*.05,-R*.07),R*.17));
     for(int ray=0;ray<12;ray++){float angle=float(ray)*6.2831853/12.0;vec2 v=vec2(sin(angle),cos(angle));
       c=dcPaint(c,vec3(.50,.27,.06),dcLine(local,v*R*.33,v*R*.47,R*.018));}
   }
   c=dcPaint(c,vec3(1.0,.98,.83),dcLine(local,vec2(-R*.42,-R*.62),vec2(R*.10,-R*.76),R*.018));
 }
 return vec4(c,alpha);
}
