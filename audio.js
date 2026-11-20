// Calm generative soundtrack (soft piano + pads + echo) and gentle SFX. Starts only after a user gesture.
const A={ctx:null,cfg:{mv:.6,sv:.8,mm:false,sm:false},mode:'menu',n:0,
setVol(c){this.cfg=c;this.apply()},
apply(){if(!this.ctx)return;const c=this.cfg,t=this.ctx.currentTime;this.mb.gain.setTargetAtTime(c.mm?0:c.mv*.3,t,.1);this.sb.gain.setTargetAtTime(c.sm?0:c.sv,t,.05)},
init(){if(this.ctx){if(this.ctx.state==='suspended')this.ctx.resume();return}
try{this.ctx=new(window.AudioContext||window.webkitAudioContext)()}catch(e){return}
const c=this.ctx;this.mb=c.createGain();this.sb=c.createGain();this.mb.connect(c.destination);this.sb.connect(c.destination);
this.dl=c.createDelay(1);this.dl.delayTime.value=.55;const fb=c.createGain();fb.gain.value=.4;const lp=c.createBiquadFilter();lp.type='lowpass';lp.frequency.value=1800;
this.dl.connect(lp);lp.connect(fb);fb.connect(this.dl);lp.connect(this.mb);this.apply();
setInterval(()=>this.tick(),1400)},
voice(f,type,peak,att,dur,dest){const c=this.ctx,t=c.currentTime,o=c.createOscillator(),g=c.createGain();o.type=type;o.frequency.value=f;
g.gain.setValueAtTime(.0001,t);g.gain.linearRampToValueAtTime(peak,t+att);g.gain.exponentialRampToValueAtTime(.0008,t+dur);o.connect(g);g.connect(this.mb);g.connect(this.dl);o.start(t);o.stop(t+dur+.1)},
coin(){this.seq([988,1319],60,'sine',.05,.18)},
tick(){if(!this.ctx||this.ctx.state!=='running')return;const ch=[[220,261.6,329.6],[174.6,220,261.6],[196,261.6,329.6],[196,246.9,293.7]],menu=this.mode==='menu';
if(this.n%5===0){const k=ch[(this.n/5|0)%4];k.forEach(f=>{this.voice(f/2,'sine',menu?.07:.05,3,8);this.voice(f,'triangle',.02,3.5,8)})}
if(Math.random()<(menu?.75:.5)){const s=[220,261.6,329.6,392,440,523.3,659.3];this.voice(s[Math.random()*s.length|0],'sine',menu?.1:.07,.01,3.2)}
this.n++},
tone(f,d,type='sine',v=.08,slide=0){if(!this.ctx)return;const c=this.ctx,t=c.currentTime,o=c.createOscillator(),g=c.createGain();
o.type=type;o.frequency.setValueAtTime(f,t);if(slide)o.frequency.exponentialRampToValueAtTime(Math.max(20,f+slide),t+d);
g.gain.setValueAtTime(v,t);g.gain.exponentialRampToValueAtTime(.001,t+d);o.connect(g);g.connect(this.sb);o.start(t);o.stop(t+d)},
sfx(f,d,type,v,slide){this.tone(f,d,type,v,slide)},
seq(a,gap,type='sine',v=.07,d=.3){a.forEach((f,i)=>setTimeout(()=>this.tone(f,d,type,v),i*gap))},
step_(){this.tone(110+Math.random()*30,.06,'sine',.025)},
key(){this.seq([784,1175],90,'sine',.07,.35)},
door(){this.tone(180,.6,'triangle',.06,260);setTimeout(()=>this.tone(523,.4,'sine',.06),300)},
sw(){this.tone(220,.08,'triangle',.08);setTimeout(()=>this.tone(330,.2,'sine',.07),70)},
solved(){this.seq([523,659,784],110)},
trap(){this.tone(130,.18,'triangle',.05,-60)},
die(){this.tone(330,.7,'sine',.07,-240)},
warn(){this.seq([880,880,698],170,'sine',.05,.25)},
chaos(){this.tone(110,.6,'sine',.08,300)},
wall(){this.tone(90,.5,'sine',.07,160);setTimeout(()=>this.tone(140,.3,'triangle',.04),120)},
mech(){this.seq([392,523],80,'triangle',.05,.2)},
tick(){this.tone(1000,.04,'sine',.025)},
click(){this.tone(480,.05,'sine',.05)},
win(){this.seq([523,659,784,1047],130,'sine',.07,.5)},
ach(){this.seq([659,880,1319],110,'sine',.06,.5)}};
