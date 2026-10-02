// Reusable procedural primitives. Each component receives resolved bounds and typed parameters.
bool box(vec2 p,vec2 c,vec2 h){return all(lessThan(abs(p-c),h));}
float lineDistance(vec2 p,vec2 a,vec2 b){vec2 d=b-a;return length(p-a-d*clamp(dot(p-a,d)/dot(d,d),0.0,1.0));}
vec4 slotSymbol(vec2 q,int id){
    vec3 ink=vec3(.24,.10,.17),gold=vec3(1.0,.71,.16);vec4 c=vec4(0.0);
    if(id==0){
        float d=min(max(abs(q.x)-.68,abs(q.y+.58)-.18),lineDistance(q,vec2(.48,-.48),vec2(-.24,.73))-.20);
        if(d<.09)c=vec4(ink,1);if(d<.04)c=vec4(gold,1);if(d<-.02)c=vec4(.9,.13,.25,1);if(box(q,vec2(-.17,-.66),vec2(.47,.035)))c=vec4(1,.65,.55,1);
    } else if(id==1){
        float d=abs(q.x)*.88+abs(q.y)*.7;
        if(d<.79)c=vec4(ink,1);if(d<.70)c=vec4(q.x<0?vec3(.12,.72,.86):vec3(.13,.40,.80),1);
        if(d<.70&&q.y<-.10)c=vec4(.55,.98,1,1);if(d<.32)c=vec4(.25,.88,1,1);
    } else if(id==2){
        bool body=(length(vec2(q.x,q.y+.10))<.59&&q.y<.15)||box(q,vec2(0,.16),vec2(.58,.36));
        if(body||box(q,vec2(0,.5),vec2(.76,.16))||length(q-vec2(0,.72))<.16)c=vec4(ink,1);
        if((body&&abs(q.x)<.49)||box(q,vec2(0,.5),vec2(.66,.08)))c=vec4(q.x<0?vec3(1,.82,.22):vec3(.91,.49,.06),1);
        if(lineDistance(q,vec2(-.24,-.36),vec2(-.31,.13))<.05)c=vec4(1,1,.69,1);
    } else if(id==3){
        if(min(lineDistance(q,vec2(-.38,.28),vec2(.17,-.65)),lineDistance(q,vec2(.38,.38),vec2(.17,-.65)))<.055)c=vec4(.18,.48,.14,1);
        if(length((q-vec2(.36,-.60))*vec2(1,2.6))<.31)c=vec4(.26,.72,.28,1);
        for(int j=0;j<2;j++){vec2 v=q-vec2(j==0?-.37:.37,j==0?.32:.44);float d=length(v);if(d<.40)c=vec4(ink,1);if(d<.33)c=vec4(v.x<0?vec3(1,.18,.31):vec3(.69,.06,.19),1);if(length(v-vec2(-.1,-.12))<.075)c=vec4(1,.80,.67,1);}
    } else if(id==4){
        vec2 v=mat2(.93,-.36,.36,.93)*q;float d=length(v*vec2(1,1.45));
        if(d<.78)c=vec4(ink,1);if(d<.69)c=vec4(1,.79,.13,1);if(d<.53&&v.y<-.1)c=vec4(1,.98,.51,1);
    } else {
        if(box(q,vec2(0),vec2(.84,.48)))c=vec4(ink,1);if(box(q,vec2(0),vec2(.76,.40)))c=vec4(.38,.19,.37,1);
        // Three tiny 3x5 glyphs, B / A / R, embedded as bit masks.
        const int glyphs[3]=int[3](27566,23530,23470);
        for(int j=0;j<3;j++){vec2 v=(q-vec2(-.65+float(j)*.46,-.27))/.115;ivec2 cell=ivec2(floor(v));if(cell.x>=0&&cell.x<3&&cell.y>=0&&cell.y<5&&((glyphs[j]>>(cell.y*3+2-cell.x))&1)!=0)c=vec4(1,.90,.61,1);}
    }
    return c;
}

float roundRect(vec2 p,vec2 halfSize,float radius){vec2 d=abs(p)-halfSize+radius;return length(max(d,vec2(0)))+min(max(d.x,d.y),0.0)-radius;}
vec3 cardPalette(int id){return id==1?vec3(.19,.64,.53):id==2?vec3(.89,.38,.43):id==3?vec3(.56,.42,.77):vec3(.15,.29,.37);}
bool rankGlyph(vec2 p,int rank){
    // Compact 3x5 lettering; T denotes ten, preserving legibility at small GUI scales.
    int bits=0;
    if(rank==2)bits=7|(4<<3)|(7<<6)|(1<<9)|(7<<12);
    if(rank==3)bits=7|(4<<3)|(7<<6)|(4<<9)|(7<<12);
    if(rank==4)bits=5|(5<<3)|(7<<6)|(4<<9)|(4<<12);
    if(rank==5)bits=7|(1<<3)|(7<<6)|(4<<9)|(7<<12);
    if(rank==6)bits=7|(1<<3)|(7<<6)|(5<<9)|(7<<12);
    if(rank==7)bits=7|(4<<3)|(4<<6)|(2<<9)|(2<<12);
    if(rank==8)bits=7|(5<<3)|(7<<6)|(5<<9)|(7<<12);
    if(rank==9)bits=7|(5<<3)|(7<<6)|(4<<9)|(7<<12);
    if(rank==10)bits=7|(2<<3)|(2<<6)|(2<<9)|(2<<12);
    if(rank==11)bits=4|(4<<3)|(4<<6)|(5<<9)|(7<<12);
    if(rank==12)bits=7|(5<<3)|(5<<6)|(3<<9)|(6<<12);
    if(rank==13)bits=5|(5<<3)|(3<<6)|(5<<9)|(5<<12);
    if(rank==14)bits=2|(5<<3)|(7<<6)|(5<<9)|(5<<12);
    ivec2 cell=ivec2(floor(p));return cell.x>=0&&cell.x<3&&cell.y>=0&&cell.y<5&&((bits>>(cell.y*3+cell.x))&1)!=0;
}
bool cardSuit(vec2 p,int suit){
    if(suit==1)return abs(p.x)*.8+abs(p.y)<.83;
    if(suit==2)return (length(p-vec2(-.32,-.25))<.43||length(p-vec2(.32,-.25))<.43||(p.y>=-.18&&p.y<.78&&abs(p.x)<(.78-p.y)*.85));
    if(suit==3){vec2 h=vec2(p.x,-p.y);return (length(h-vec2(-.32,-.12))<.39||length(h-vec2(.32,-.12))<.39||(h.y>=-.12&&h.y<.85&&abs(h.x)<(.85-h.y)*.75))||box(p,vec2(0,.60),vec2(.13,.25));}
    return length(p-vec2(0,-.40))<.34||length(p-vec2(-.34,.12))<.35||length(p-vec2(.34,.12))<.35||box(p,vec2(0,.56),vec2(.12,.28));
}
vec4 playingCard(vec2 q,vec2 size,int a,int b,float t,bool live){
    int id=a&63,mode=(a>>13)&3,palette=(b>>13)&3;bool down=((a>>6)&1)!=0,highlighted=((a>>7)&1)!=0;
    float delay=float((a>>8)&31)/10.0,duration=max(.05,float(b&127)/20.0),lift=float((b>>7)&63);
    float u=live?clamp((t-delay)/duration,0.0,1.0):1.0;
    if((mode==1||mode==3)&&live&&t<delay)return vec4(0);
    float h=size.y-lift-6.0,w=min(size.x-4.0,h*.66),compression=1.0,angle=0.0;
    vec2 center=vec2(size.x*.5,lift+h*.5+1.0);
    bool back=down;
    if(mode==3){
        h=lift;w=min(size.x-4.0,h*.66);
        vec2 source=vec2(size.x-w*.5-3.0,h*.5+3.0),dest=vec2(w*.5+2.0,size.y-h*.5-5.0);
        float e=1.0-pow(1.0-u,3.0);center=mix(source,dest,e);
        center.y-=sin(u*3.141593)*min(10.0,max(0.0,(size.y-h)*.15));angle=(1.0-u)*.12;
    }
    if(mode==1){center.y-=lift*pow(1.0-u,3.0);angle=(1.0-u)*-.14;}
    if(mode==2){compression=max(.045,abs(cos(u*3.141593)));if(u<.5)back=!down;center.y-=sin(u*3.141593)*3.0;}
    vec2 p=q-center;p=mat2(cos(angle),sin(angle),-sin(angle),cos(angle))*p;
    p.x/=compression;vec2 halfSize=vec2(w,h)*.5;float r=max(1.1,w*.075),dist=roundRect(p,halfSize,r);
    vec4 c=vec4(0);
    float shadow=roundRect((q-center-vec2(1.3,2.8))/vec2(compression,1),halfSize,r);
    if(shadow<1.0)c=vec4(.025,.04,.055,.55*(1.0-smoothstep(-1.0,1.0,shadow)));
    if(dist<=0.0){
        c=vec4(mix(vec3(.96,.92,.83),vec3(1,.985,.94),clamp(.6-p.y/h,0.0,1.0)),1);
        if(dist>-.8)c.rgb=highlighted?vec3(1,.80,.39):vec3(.72,.69,.60);
        if(back){
            c.rgb=cardPalette(palette);if(dist> -1.6)c.rgb=vec3(.94,.78,.46);
            float weave=mod(floor((p.x+p.y)/3.0)+floor((p.x-p.y)/3.0),2.0);
            if(dist< -2.5)c.rgb*=.88+weave*.15;
            float diamond=abs(p.x)*.8+abs(p.y)*.6;
            if(diamond<w*.25&&diamond>w*.19)c.rgb=vec3(.95,.82,.55);
            if(length(p)<w*.08)c.rgb=vec3(.93,.96,.86);
        }else if(id<52){
            int rank=id%13+2,suit=id/13;vec3 ink=(suit==1||suit==2)?vec3(.86,.25,.33):vec3(.13,.22,.29);
            float unit=max(.85,w/27.0);vec2 corner=p+halfSize-vec2(2.4,2.4);
            if(rankGlyph(corner/unit,rank)||cardSuit((corner-vec2(1.5,7.0)*unit)/(1.9*unit),suit))c.rgb=ink;
            vec2 bottom=-p+halfSize-vec2(2.4,2.4);
            if(rankGlyph(bottom/unit,rank)||cardSuit((bottom-vec2(1.5,7.0)*unit)/(1.9*unit),suit))c.rgb=ink;
            if(cardSuit(p/(w*.24),suit))c.rgb=ink;
            if(rank>=11&&rank<=13){float ring=abs(p.x)*.8+abs(p.y)*.55;if(ring>w*.31&&ring<w*.34)c.rgb=vec3(.79,.58,.27);}
        }else{
            c.rgb=vec3(.06,.20,.21);if(dist>-.7)c.rgb=vec3(.28,.44,.40);
            if(abs(p.x)*.8+abs(p.y)*.5<w*.15)c.rgb=vec3(.32,.49,.43);
        }
        if(highlighted&&dist<-.9&&dist>-2.0)c.rgb=vec3(1,.82,.43);
    }
    return c;
}
vec2 chipAnchor(int id,vec2 size){
    vec2 v=id==0?vec2(.13,.14):id==1?vec2(.87,.14):id==2?vec2(.13,.86):id==3?vec2(.87,.86):id==4?vec2(.5,.14):id==5?vec2(.5,.86):id==6?vec2(.13,.5):vec2(.87,.5);return v*size;
}
vec4 chipDisc(vec2 p,float radius,vec3 paint){
    vec4 c=vec4(0);float d=length(p/vec2(radius,radius*.44));
    float edge=length((p-vec2(0,1.5))/vec2(radius,radius*.44));
    if(edge<1.06)c=vec4(paint*.42,1);
    if(d<1.0){c=vec4(paint,1);float angle=atan(p.y/.44,p.x);if(d>.73&&d<.95&&cos(angle*6.0)>.0)c.rgb=vec3(1,.94,.79);if(d<.54&&d>.42)c.rgb=vec3(1,.94,.79);if(d<.32)c.rgb=mix(paint,vec3(1,.96,.85),.23);}
    return c;
}
vec4 chipStack(vec2 q,vec2 size,int a,int b,float t,bool live){
    int count=a&31,palette=(a>>5)&3,mode=(b>>7)&3;float duration=max(.05,float(b&127)/20.0),delay=float((b>>9)&63)/10.0;
    if(count==0)return vec4(0);
    vec3 paint=palette==1?vec3(.35,.83,.66):palette==2?vec3(.94,.36,.43):palette==3?vec3(.60,.45,.87):vec3(.96,.73,.33);
    vec2 dest=chipAnchor((a>>10)&7,size),source=chipAnchor((a>>7)&7,size);float radius=clamp(min(size.x,size.y)*.085,2.2,5.2);vec4 c=vec4(0);
    int layers=min(count,7);
    for(int j=0;j<7;j++){
        if(j>=layers)break;
        float u=live&&mode==1?clamp((t-delay-float(j)*.035)/duration,0.0,1.0):1.0;
        vec2 at=mode==1?mix(source,dest,1.0-pow(1.0-u,3.0)):size*.5;
        if(mode==1)at.y-=sin(u*3.141593)*min(18.0,size.y*.25);
        at+=vec2(sin(float(j)*2.0)*(1.0-u)*radius*.5,-float(j)*1.1);
        vec4 chip=chipDisc(q-at,radius,paint);if(chip.a>0)c=chip;
    }
    return c;
}

// Single-zero wheel is a rendering preset; randomness, bets and payouts live in the consumer.
const float wheelTau=6.28318530718;
const int wheelNumbers[37]=int[37](0,32,15,19,4,21,2,25,17,34,6,27,13,36,11,30,8,23,10,5,24,16,33,1,20,14,31,9,22,18,29,7,28,12,35,3,26);
int wheelIndex(int value){for(int i=0;i<37;i++)if(wheelNumbers[i]==value)return i;return 0;}
bool wheelRed(int n){return n==1||n==3||n==5||n==7||n==9||n==12||n==14||n==16||n==18||n==19||n==21||n==23||n==25||n==27||n==30||n==32||n==34||n==36;}
bool wheelDigit(vec2 p,int digit,bool detailed){
    ivec2 cell=ivec2(floor(p));
    if(cell.x<0||cell.y<0||cell.x>=(detailed?5:3)||cell.y>=(detailed?7:5))return false;
    if(detailed){
        // 5x7 numerals: distinct diagonals and a stem/base on one.
        // Rows are left-to-right, with the leftmost pixel in the low bit.
        const int rows[70]=int[70](
            14,17,17,17,17,17,14, 4,6,4,4,4,4,14,
            14,17,16,8,4,2,31, 15,16,16,14,16,16,15,
            8,12,10,9,31,8,8, 31,1,1,15,16,16,15,
            14,1,1,15,17,17,14, 31,16,8,4,2,2,2,
            14,17,17,14,17,17,14, 14,17,17,30,16,16,14);
        return ((rows[clamp(digit,0,9)*7+cell.y]>>cell.x)&1)!=0;
    }
    // Small wheels retain a 3x5 font rather than collapsing the extra columns.
    const int digits[10]=int[10](31599,29842,29671,31207,18925,31183,31695,18727,31727,31215);
    return ((digits[clamp(digit,0,9)]>>(cell.y*3+cell.x))&1)!=0;
}
bool wheelLabel(vec2 p,int number,bool detailed){
    if(number<10)return wheelDigit(p,number,detailed);
    return wheelDigit(p,number/10,detailed)||wheelDigit(p-vec2(detailed?6:4,0),number%10,detailed);
}
float wheelLabelCoverage(vec2 p,int number,bool detailed,vec2 footprint){
    // Four screen-space samples smooth rotated strokes without seams between filled cells.
    return .25*(float(wheelLabel(p+footprint,number,detailed))
        +float(wheelLabel(p-footprint,number,detailed))
        +float(wheelLabel(p+vec2(footprint.x,-footprint.y),number,detailed))
        +float(wheelLabel(p+vec2(-footprint.x,footprint.y),number,detailed)));
}
vec3 wheelBrass(float r,float theta){
    float spec=pow(max(0.0,cos(theta+.9)),8.0),groove=.04*sin(r*650.0);
    return mix(vec3(.40,.28,.12),vec3(.88,.75,.46),.50+.28*cos(theta+.9))+spec*vec3(.18,.16,.11)+groove;
}
vec4 rouletteWheel(vec2 q,vec2 size,int a,int b,float t,bool live){
    float radius=size.x*.47,stepAngle=wheelTau/37.0;
    vec2 p=(q-size*.5)/radius;float r=length(p),theta=atan(p.x,-p.y);
    int value=a&63,previous=(a>>6)&63,turns=(a>>12)&7,mode=(b>>9)&1,palette=(b>>10)&1;
    float duration=float(b&511)/20.0,u=live&&mode==1?clamp(t/duration,0.0,1.0):1.0;
    float start=-float(wheelIndex(previous))*stepAngle;
    float travel=float(turns)*wheelTau+mod(float(wheelIndex(previous)-wheelIndex(value))*stepAngle,wheelTau);
    float wheel=start+travel*(1.0-pow(1.0-u,3.0));
    float angle=mod(theta-wheel+stepAngle*.5,wheelTau),sector=floor(angle/stepAngle);
    int number=wheelNumbers[int(sector)];float localAngle=mod(angle,stepAngle)-stepAngle*.5;
    bool detailed=radius>=80.0;
    float columns=detailed?5.0:3.0,rows=detailed?7.0:5.0;
    // Fit BOTH digits and their gap within the central 72% of the pocket angle.
    // Square glyph cells preserve the font's natural proportions in both GUI profiles.
    // Fit the rectangle at its INNER edge, where adjacent pockets are closest together.
    float halfLabelAngle=stepAngle*.36;
    float cellSize=min(2.0*.735*tan(halfLabelAngle)
        /(columns*2.0+1.0+rows*tan(halfLabelAngle)),.105/rows);
    vec2 unit=vec2(cellSize);
    vec2 label=vec2(sin(localAngle)*r,.735-cos(localAngle)*r)/unit
        +vec2(number<10?columns*.5:columns+.5,rows*.5);
    // Derivatives precede the radial branches; sector wrap cannot widen the filter arbitrarily.
    vec2 labelFootprint=clamp(fwidth(label),vec2(.001),vec2(1.0))*.25;
    vec4 c=vec4(0);
    float shadow=length((p-vec2(.018,.028))/vec2(1.0,.995));
    if(shadow<1.055)c=vec4(.025,.035,.03,.7*(1.0-smoothstep(1.0,1.055,shadow)));
    if(r<1.0){
        float grain=sin(theta*25.0+r*120.0)*.008;
        c=vec4(palette==1?vec3(.12,.135,.135):vec3(.32,.18,.105),1);
        c.rgb*=.83+.16*cos(theta+.8);c.rgb+=grain;
        if(r>.986)c.rgb=vec3(.13,.10,.07);
        if(r>.958&&r<.983)c.rgb=wheelBrass(r,theta);
        if(r>.838&&r<.938)c.rgb=mix(vec3(.065,.085,.08),vec3(.22,.255,.225),smoothstep(.838,.938,r));
        if(r>.91&&r<.917)c.rgb=vec3(.52,.54,.44);
        if(r>.807&&r<.832)c.rgb=wheelBrass(r,theta);
        // Number band and the physically separate, inset pocket ring.
        if(r>.55&&r<.806){
            vec3 paint=number==0?vec3(.10,.38,.27):wheelRed(number)?vec3(.66,.11,.12):vec3(.055,.075,.072);
            c.rgb=paint*(r<.65?.72:1.0);
            if(r<.56)c.rgb=wheelBrass(r,theta);
            if(r>.646&&r<.657)c.rgb=wheelBrass(r,theta);
            if(abs(localAngle)>stepAngle*.47)c.rgb=wheelBrass(r,theta);
            if(r>.67&&r<.79){
                float ink=wheelLabelCoverage(label,number,detailed,labelFootprint);
                c.rgb=mix(c.rgb,vec3(1.0,.98,.90),ink);
            }
        }
        if(r<.545){
            float rotorTheta=theta-wheel;
            c.rgb=palette==1?vec3(.09,.11,.105):vec3(.32,.195,.115);
            c.rgb*=.76+.19*cos(rotorTheta*2.0)+.05*cos(rotorTheta*20.0+r*80.0);
            if(r>.512)c.rgb=wheelBrass(r,theta);
            // A conical brass turret with four slender spindle arms.
            if(r>.125&&r<.29&&abs(sin(rotorTheta*2.0))<.07/r)c.rgb=wheelBrass(r,theta);
            if(r<.152)c.rgb=wheelBrass(r,theta)*(.78+.4*(1.0-r/.152));
            if(r<.064)c.rgb=mix(vec3(.47,.35,.18),vec3(.98,.87,.61),clamp(.6-p.x*7.0-p.y*6.0,0.0,1.0));
            if(r>.043&&r<.05)c.rgb=vec3(.32,.23,.10);
        }
        // Eight fixed low-profile brass deflectors on the sloped ball track.
        float deflector=abs(mod(theta+wheelTau/16.0,wheelTau/8.0)-wheelTau/16.0);
        if(abs(r-.852)<.014&&deflector<.018)c.rgb=wheelBrass(r,theta);
    }
    // Counter-rotation, centrifugal track, progressive drop, damped pocket bounce and capture.
    float capture=.74,ballAngle=0.0,ballR=.60;
    if(u<1.0){
        float cycle=float(turns*2+2)*wheelTau;
        float orbit=-cycle*(1.0-pow(1.0-u,2.0));
        float orbitAt=-cycle*(1.0-pow(1.0-capture,2.0));
        float wheelAt=start+travel*(1.0-pow(1.0-capture,3.0));
        float offset=mod(orbitAt-wheelAt-float(wheelIndex(value))*stepAngle+3.141593,wheelTau)-3.141593;
        if(u<capture)ballAngle=orbit;
        else {float v=(u-capture)/(1.0-capture);ballAngle=wheel+float(wheelIndex(value))*stepAngle+offset*pow(1.0-v,3.0)+sin(v*20.0)*.018*pow(1.0-v,2.0);}
        float drop=smoothstep(.55,.82,u);ballR=mix(.89,.60,drop)+sin(u*115.0)*.015*sin(drop*3.141593);
    }
    vec2 ball=vec2(sin(ballAngle),-cos(ballAngle))*ballR;
    float ballSize=max(.018,1.7/radius),ds=length((p-ball-vec2(.008,.012))/vec2(ballSize*1.1,ballSize*.7));
    if(ds<1.25)c=mix(c,vec4(.03,.04,.035,1),.48*(1.0-smoothstep(.5,1.25,ds)));
    vec2 bp=(p-ball)/ballSize;float bd=length(bp);
    if(bd<1.0)c=vec4(mix(vec3(.56,.58,.53),vec3(1.0,.99,.91),clamp(.75-bp.y*.35-bp.x*.25,0.0,1.0)),1);
    if(length(bp-vec2(-.30,-.34))<.24)c=vec4(1,1,.98,1);
    // Fixed ivory marker at twelve o'clock. The winning pocket and ball both settle beneath it.
    if(p.y<-.972&&p.y>-1.034&&abs(p.x)<(p.y+1.034)*.40)c=vec4(.98,.91,.72,1);
    return c;
}

vec4 demoEffectPixel(vec2 q,vec2 size,int kind,int a,int b,float age,bool motion,bool eventLive){
    if(any(lessThan(q,vec2(0)))||any(greaterThanEqual(q,size)))return vec4(0);
    float t=eventLive&&motion?max(0.0,age):100.0;vec4 c=vec4(0);

    if(kind==0)return rouletteWheel(q,size,a,b,t,eventLive&&motion);
    if(kind==6)return playingCard(q,size,a,b,t,eventLive&&motion);
    if(kind==7)return chipStack(q,size,a,b,t,eventLive&&motion);
    if(kind==1){
        float shade=.92-.17*pow(abs(q.y/size.y-.5)*2.0,2.0);c=vec4(vec3(1,.96,.83)*shade,1);
        if(q.x<2.0||q.x>size.x-2.0)c=vec4(.70,.52,.35,1);
        int target=a&7,prev=(a>>3)&7;float duration=float(b&127)/20.0,symbolSize=float((b>>7)&127);
        float u=clamp(t/duration,0.0,1.0),distance=float((a>>6)&63)+mod(float(target-prev+6),6.0);
        float pos=float(prev)+distance*(1.0-pow(1.0-u,3.0));if(!eventLive||!motion||t>=duration)pos=float(target);
        float cell=symbolSize*2.4,yy=(q.y-size.y*.5)/cell+pos,row=floor(yy+.5);int id=int(mod(row,6.0));
        vec4 symbol=slotSymbol(vec2((q.x-size.x*.5)/symbolSize,(yy-row)*cell/symbolSize),id);if(symbol.a>0.0)c=symbol;
        c.rgb*=1.0-.20*pow(abs(q.y/size.y-.5)*2.0,6.0);
        if(abs(q.y-size.y*.5)<1.0&&(q.x<6.0||q.x>size.x-6.0))c=vec4(1,.63,.15,1);
    }else if(kind==2){
        vec2 pivot=size*vec2(.5,.86);float leverLength=size.y*.60,duration=float(a)/20.0;
        float pull=eventLive&&motion&&t<duration?sin(clamp(t/duration,0.0,1.0)*3.141593):0.0;
        vec2 knob=pivot+vec2(pull*size.x*.20,-leverLength+pull*leverLength*.76);
        float radius=min(size.x*.26,size.y*.15),stem=lineDistance(q,knob,pivot);
        if(stem<radius*.36)c=vec4(.18,.12,.20,1);if(stem<radius*.22)c=vec4(.70,.77,.80,1);if(stem<radius*.08)c=vec4(1,.96,.85,1);
        if(length((q-pivot)/(radius*vec2(.82,.45)))<1)c=vec4(.65,.42,.20,1);
        vec2 k=(q-knob)/radius;
        if(length(k)<1.15)c=vec4(.27,.07,.16,1);if(length(k)<1)c=vec4(mix(vec3(.66,.04,.16),vec3(1,.27,.32),clamp(.6-k.y*.4-k.x*.3,0.0,1.0)),1);
        if(length(k-vec2(-.3,-.35))<.23)c=vec4(1,.77,.62,1);
    }else if(kind==3){
        float delay=float((b>>9)&63)/10.0,elapsed=age-delay;int count=(a>>9)&63;
        if(eventLive&&motion&&elapsed>0.0&&elapsed<4.7){
            float scale=min(size.x/252.0,size.y/171.0);vec2 origin=vec2(float(a&511),float(b&511));
            for(int i=0;i<63;i++){
                if(i>=count)break;
                float seed=float(i),ct=elapsed-random(seed+84.0)*1.1;if(ct<0.0)continue;
                vec2 vel=vec2((random(seed+33.0)-.5)*220.0,-95.0-random(seed+16.0)*110.0)*scale;
                vec2 at=origin+vel*ct+vec2(0,85.0*scale*ct*ct),delta=q-at;float r=(3.8+random(seed+8.0)*1.8)*scale;
                if(any(greaterThan(abs(delta),vec2(r))))continue;
                vec2 coin=delta/vec2(r*max(.24,abs(cos(ct*8.0+seed))),r);float dist=length(coin);
                if(dist<1.0){vec3 paint=dist>.78?vec3(.66,.33,.07):dist>.63?vec3(1,.95,.55):vec3(1,.71,.16);if(abs(coin.x)<.12&&abs(coin.y)<.46)paint=vec3(1,.98,.66);c=vec4(paint,1.0-smoothstep(3.7,4.7,elapsed));}
            }
        }
    }else if(kind==5){
        float elapsed=age-float((b>>9)&63)/10.0;int count=(a>>9)&63;
        if(eventLive&&motion&&elapsed>0.0&&elapsed<3.6){
            vec2 origin=vec2(float(a&511),float(b&511));float scale=min(size.x/300.0,size.y/216.0);
            const vec3 colors[5]=vec3[5](vec3(.35,1,.73),vec3(1,.29,.56),vec3(1,.87,.39),vec3(.66,.55,1),vec3(1,.97,.85));
            for(int i=0;i<63;i++){
                if(i>=count)break;float seed=float(i),ct=elapsed-random(seed+31.0)*.18;if(ct<0.0)continue;
                vec2 vel=vec2((random(seed+23.0)-.5)*260.0,-110.0-random(seed+14.0)*90.0)*scale;
                vec2 at=origin+vel*ct+vec2(0,75.0*scale*ct*ct),d=q-at;
                if(any(greaterThan(abs(d),vec2(4.0*scale))))continue;
                float angle=ct*(3.0+random(seed+44.0)*8.0)+seed;
                d=mat2(cos(angle),sin(angle),-sin(angle),cos(angle))*d;
                vec2 halfSize=vec2(2.5*max(.3,abs(cos(ct*9.0+seed))),1.4)*scale;
                if(all(lessThan(abs(d),halfSize)))c=vec4(colors[i%5],1.0-smoothstep(2.8,3.6,elapsed));
            }
        }
    }else if(kind==4){
        float radius=float(b);for(int i=0;i<32;i++){
            if(i>=a)break;vec2 center=vec2(a==1?size.x*.5:radius+float(i)*(size.x-radius*2.0)/float(a-1),size.y*.5);
            if(length(q-center)<radius){float on=motion&&eventLive?step(.1,sin(t*9.0-float(i))):1.0;c=vec4(mix(vec3(.57,.30,.15),vec3(1,.90,.48),on),1);}
        }
    }
    return c;
}
