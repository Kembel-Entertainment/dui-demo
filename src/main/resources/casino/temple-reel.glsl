vec3 dcTempleSymbol(vec2 p,float R,int value,vec3 bg){
 vec3 c=bg,gold=dcGold(p.y+50.0),ink=vec3(.82,.66,.32);
 if(value<5){
   float s=R*.09;vec2 v=p/s;
   if(value==4){
     float ten=dcLine(v,vec2(-5,-3),vec2(-2,-6),.65);
     ten=max(ten,dcLine(v,vec2(-2,-6),vec2(-2,6),.65));
     ten=max(ten,dcLine(v,vec2(-4,6),vec2(0,6),.65));
     ten=max(ten,dcMask(abs(length((v-vec2(4,0))/vec2(.52,1.0))-6.0)-.65));
     return dcPaint(c,ink,ten);
   }
   // Large engraved A / K / Q / J with naturally proportioned vector strokes.
   float shape=0.0;
   if(value==0){shape=max(dcLine(v,vec2(-4,6),vec2(0,-6),.65),dcLine(v,vec2(0,-6),vec2(4,6),.65));shape=max(shape,dcLine(v,vec2(-2.5,2),vec2(2.5,2),.6));}
   if(value==1){shape=dcLine(v,vec2(-3,-6),vec2(-3,6),.65);shape=max(shape,dcLine(v,vec2(-3,0),vec2(3,-6),.65));shape=max(shape,dcLine(v,vec2(-3,0),vec2(3,6),.65));}
   if(value==2){shape=dcMask(abs(length(v/vec2(.7,1.0))-6.0)-.65);shape=max(shape,dcLine(v,vec2(1,3),vec2(4,7),.65));}
   if(value==3){shape=dcLine(v,vec2(3,-6),vec2(3,3),.65);shape=max(shape,dcLine(v,vec2(-3,-6),vec2(3,-6),.65));shape=max(shape,dcMask(abs(length(v-vec2(0,3))-3.0)-.65)*float(v.y>=3.0));}
   return dcPaint(c,ink,shape);
 }
 if(value==5){ // Azure lotus: five overlapping carved petals.
   for(int i=-2;i<=2;i++){float angle=float(i)*.46;vec2 centre=vec2(sin(angle),-cos(angle))*R*.18;
     vec2 d=(p-centre)/vec2(R*.22,R*.44);
     c=dcPaint(c,i%2==0?vec3(.22,.79,.85):vec3(.14,.48,.72),dcMask((length(d)-1.0)*R));}
   c=dcPaint(c,gold,dcLine(p,vec2(-R*.45,R*.27),vec2(R*.45,R*.27),R*.045));
 }else if(value==6){ // Turquoise scarab, metallic wings and six legs.
   for(int i=-1;i<=1;i++){c=dcPaint(c,gold,dcLine(p,vec2(-R*.25,float(i)*R*.18),vec2(-R*.65,float(i)*R*.30),R*.03));c=dcPaint(c,gold,dcLine(p,vec2(R*.25,float(i)*R*.18),vec2(R*.65,float(i)*R*.30),R*.03));}
   c=dcPaint(c,vec3(.10,.56,.47),dcBox(p,vec2(0,R*.02),vec2(R*.32,R*.45),R*.25));
   c=dcPaint(c,gold,dcLine(p,vec2(0,-R*.38),vec2(0,R*.43),R*.035));
   c=dcPaint(c,vec3(.22,.75,.63),dcCircle(p,vec2(0,-R*.38),R*.21));
 }else if(value==7){ // Original explorer medallion, fedora and ruby compass.
   c=dcPaint(c,gold,dcCircle(p,vec2(0),R*.58));
   c=dcPaint(c,vec3(.91,.67,.42),dcCircle(p,vec2(0,R*.10),R*.29));
   c=dcPaint(c,vec3(.32,.16,.09),dcBox(p,vec2(0,-R*.16),vec2(R*.34,R*.22),R*.07));
   c=dcPaint(c,vec3(.46,.24,.12),dcBox(p,vec2(0,-R*.02),vec2(R*.46,R*.08),R*.025));
   c=dcPaint(c,vec3(.08,.10,.15),dcLine(p,vec2(-R*.15,R*.14),vec2(R*.15,R*.14),R*.025));
 }else{ // The gilded book is both scatter and wild.
   c=dcPaint(c,gold,dcBox(p,vec2(0),vec2(R*.53,R*.63),R*.065));
   c=dcPaint(c,vec3(.38,.095,.09),dcBox(p,vec2(R*.04,-R*.03),vec2(R*.42,R*.52),R*.045));
   c=dcPaint(c,gold,dcLine(p,vec2(-R*.31,-R*.49),vec2(-R*.31,R*.48),R*.025));
   c=dcPaint(c,gold,dcCircle(p,vec2(R*.07,0),R*.21));
   c=dcPaint(c,vec3(.18,.46,.65),dcCircle(p,vec2(R*.07,0),R*.115));
   c=dcPaint(c,vec3(.99,.94,.72),dcCircle(p,vec2(R*.02,-R*.05),R*.035));
 }
 return c;
}
vec4 demoTempleReel(vec2 q,vec2 size,int a,int b,float t,bool live){
 int reel=(a>>12)&7;bool spin=(b&512)!=0,expanded=(b&1024)!=0;
 float u=spin&&live?clamp(t/(float(b&511)/20.0),0.0,1.0):1.0;
 float h=(size.y-8.0)/3.0,scroll=pow(1.0-u,3.0)*(14.0+float(reel)*3.0);
 float logical=(q.y-4.0)/h+scroll;int row=int(floor(logical));
 int actualRow=clamp(row,0,2),symbol=(a>>(actualRow*4))&15;
 if(scroll>.01)symbol=(row+int(floor(scroll))*3+reel*2+18)%9;
 vec2 p=vec2(q.x-size.x*.5,(fract(logical)-.5)*h);
 vec3 c=mix(vec3(.055,.095,.14),vec3(.10,.16,.21),q.y/size.y);
 c=dcTempleSymbol(p,min(h*.78,size.x*.75),symbol,c);
 if(expanded&&u>.98){c=dcPaint(c,dcGold(q.y),dcLine(q,vec2(4,4),vec2(4,size.y-4),1.2));c=dcPaint(c,dcGold(q.y),dcLine(q,vec2(size.x-4,4),vec2(size.x-4,size.y-4),1.2));}
 if(!spin&&((b>>(11+actualRow))&1)!=0)c=dcPaint(c,vec3(1.0,.84,.36),dcMask(abs(abs(p.y)-h*.45)-.7));
 c=dcPaint(c,dcGold(q.y),1.0-dcBox(q,size*.5,size*.5-vec2(2),2.0));
 return vec4(c,1.0);
}
