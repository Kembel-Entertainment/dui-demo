vec4 demoGiftConfetti(vec2 q,vec2 size,DUI_PARAMETERS p,float t,bool live){vec4 color=vec4(0);if(!live)return color;
        // Two cartoon confetti cannons. One finite burst, driven by world ticks, no per-frame packets.
        if(t<4.8) {
            const vec3 palette[6]=vec3[6](vec3(1.0,.30,.51),vec3(1.0,.73,.14),vec3(.16,.79,.59),
                vec3(.22,.64,1.0),vec3(.65,.39,.97),vec3(1.0,.49,.20));
            float scale=size.x/480.0;
            for(int i=0;i<54;i++) {
                float seed=float(i),t=t-random(seed+4.0)*.24;
                if(t<0.0)continue;
                float side=mod(seed,2.0),direction=side<.5?1.0:-1.0;
                vec2 origin=vec2(mix(.13,.87,side),.81)*size;
                vec2 velocity=vec2(direction*(.07+random(seed+13.0)*.24)*size.x,
                    -(.70+random(seed+27.0)*.44)*size.y);
                vec2 pos=origin+velocity*t+vec2(sin(t*3.0+seed)*5.0*scale,.27*size.y*t*t);
                vec2 delta=q-pos;
                // Reject distant fragments before the rotation and shape work.
                if(any(greaterThan(abs(delta),vec2(8.0*scale))))continue;
                float angle=seed+t*(2.0+random(seed+2.0)*5.0);
                vec2 q=mat2(cos(angle),-sin(angle),sin(angle),cos(angle))*delta;
                vec2 halfSize=vec2((2.2+random(seed)*1.2)*max(.25,abs(cos(t*5.0+seed))),3.8+random(seed+1.0)*1.8)*scale;
                if(any(greaterThan(abs(q),halfSize)))continue;
                float fade=(1.0-smoothstep(3.5,4.8,t))*smoothstep(0.0,.08,t);
                vec3 paint=palette[i%6];
                if(any(greaterThan(abs(q),halfSize-vec2(.65*scale))))paint*=.65;
                // Straight-alpha composition also retains the mascot underneath falling paper.
                float alpha=fade+color.a*(1.0-fade);
                color=vec4((paint*fade+color.rgb*color.a*(1.0-fade))/max(alpha,.0001),alpha);
            }
        }
return color;}
