'use strict';
const TS=32,COLS=15,ROWS=11,VW=COLS*TS,VH=ROWS*TS,SPD=110,HB=8,WARN=1.8;
const DIRS=[[0,-1],[1,0],[0,1],[-1,0]],DN=['UP','RIGHT','DOWN','LEFT'];
const CH={reverse:{d:5,c:'#ff5c8a'},gravity:{d:5,c:'#6cf'},invis:{d:3.5,c:'#b7f'},walls:{d:6,c:'#c6f'},color:{d:5,c:'#fc4'},freeze:{d:1.3,c:'#8ef'},dark:{d:7,c:'#aaa'}};
const $=s=>document.querySelector(s),cv=$('#c'),cx=cv.getContext('2d');
const dk=document.createElement('canvas');dk.width=VW;dk.height=VH;const dx=dk.getContext('2d');
let save={unlocked:1,best:{},mv:.6,sv:.8,mm:false,sm:false,seen:false,name:'',rs:{},ach:{},sec:{},att:{},chaosN:0,lefty:false,lang:null,ver:0,auto:true};
try{Object.assign(save,JSON.parse(localStorage.getItem('chaosroom.v1')||'{}'))}catch(e){}
const persist=()=>{try{localStorage.setItem('chaosroom.v1',JSON.stringify(save))}catch(e){}};
if(!save.pid)save.pid=Math.random().toString(36).slice(2)+Date.now().toString(36);A.setVol(save);if(save.lefty)document.body.classList.add('lefty');
if(save.ver<3){save.ver=3;for(const k of [5,6]){delete save.best[k];delete save.rs[k];delete save.att[k];delete save.sec[k]}save.unlocked=Math.min(save.unlocked,6);persist()}
I18N.set(save.lang||((navigator.language||'en').toLowerCase().startsWith('ar')?'ar':'en'));
let scene='menu',S=null,last=0,jx=0,jy=0,actEdge=false,prevScene='menu',roomCompletionCounted=false;const kd=new Set();
const fmt=t=>t.toFixed(1)+tr('s');
// ---------- scenes / UI ----------
const SCREEN={menu:'menu',levels:'levels',settings:'settings',pause:'pause',over:'over',done:'done',victory:'victory',lb:'lb',how:'how',tut:'tut'};
function setScene(s){scene=s;document.body.dataset.scene=s;document.querySelectorAll('.screen').forEach(e=>e.style.display='none');
const id=SCREEN[s];if(id)$('#'+id).style.display='flex';
$('#vm').value=save.mv*100;$('#vs').value=save.sv*100;refreshLabels();
if(s==='levels')buildLevels();if(s==='lb')buildLb();if(s==='tut')tutGo(0);A.mode=(s==='play'||s==='pause'||s==='over'||s==='done'||s==='victory')?'game':'menu'}
function refreshLabels(){$('#bm').textContent=tr('music')+': '+tr(save.mm?'muted':'on');$('#bs').textContent=tr('sfx')+': '+tr(save.sm?'muted':'on');$('#blf').textContent=tr('lefty')+': '+tr(save.lefty?'on':'off');$('#bau').textContent=tr('auto')+': '+tr(save.auto!==false?'on':'off');
document.querySelectorAll('[data-l]').forEach(b=>b.classList.toggle('sel',b.dataset.l===I18N.lang))}
function buildLevels(){const l=$('#lvl');l.innerHTML='';LEVELS.forEach((L,i)=>{const b=document.createElement('button'),ok=i<save.unlocked,done=!!save.rs[i];b.disabled=!ok;b.dataset.a='sel';b.dataset.i=i;b.className='card2 d'+L.d+(done?' done':'');
b.innerHTML='<small>'+tr('room',{n:i+1})+'</small><b>'+tr('r'+(i+1)+'n')+'</b><em>'+tr('diff'+L.d)+'</em><span>'+(ok?ico(done?'check':'play',12)+(save.best[i]?fmt(save.best[i]):tr('open')):ico('lock',12)+tr('locked'))+'</span>';l.appendChild(b)})}
function toast(t,d=2.5){S.toast={t:d,txt:t};}
function start(i){if(i>=save.unlocked||!LEVELS[i])return;roomCompletionCounted=false;const f=$('#fade');f.style.transition='none';f.style.opacity=1;requestAnimationFrame(()=>requestAnimationFrame(()=>{f.style.transition='opacity .6s';f.style.opacity=0}));loadLevel(i);buildStatic();S.cx=Math.max(0,Math.min(S.W*TS-VW,S.px-VW/2));S.cy=Math.max(0,Math.min(S.H*TS-VH,S.py-VH/2));setScene('play');toast(tr('r'+(i+1)+'h'),4);const rc=$('#rc');rc.innerHTML='<small>'+tr('room',{n:i+1})+'</small><b>'+tr('r'+(i+1)+'n')+'</b>';rc.classList.remove('show');void rc.offsetWidth;rc.classList.add('show')}
document.addEventListener('click',e=>{const b=e.target.closest('[data-a]');if(!b||b.disabled)return;A.init();A.click();
const a=b.dataset.a;
if(a==='play')start(Math.min(save.unlocked,LEVELS.length)-1);
else if(a==='levels')setScene('levels');else if(a==='settings'){prevScene='menu';setScene('settings')}
else if(a==='settings2'){prevScene='pause';setScene('settings')}
else if(a==='menu'){if(scene==='settings'&&prevScene==='pause'){setScene('pause');prevScene='menu'}else{S=null;setScene('menu')}}
else if(a==='sel')start(+b.dataset.i);else if(a==='pause')pause();else if(a==='resume')setScene('play');
else if(a==='restart')start(S.i);else if(a==='retry'){respawn();setScene('play')}
else if(a==='next')CHAOS_ADS.showNextRoom(()=>start(S.i+1));
else if(a==='music'){save.mm=!save.mm;A.apply();persist();setScene(scene)}
else if(a==='sfx'){save.sm=!save.sm;A.apply();persist();setScene(scene)}
else if(a==='reset'){if(confirm(tr('confirm'))){Object.assign(save,{unlocked:1,best:{},rs:{},ach:{},sec:{},att:{},chaosN:0});persist();toast_(tr('erased'))}}
else if(a==='auto'){save.auto=save.auto===false;persist();setScene(scene)}
else if(a==='lang'){save.lang=b.dataset.l;persist();I18N.set(save.lang);setScene(scene)}
else if(a==='lb')setScene('lb');else if(a==='how')setScene('how');else if(a==='tut')setScene('tut');
else if(a==='tnext'){ti===TUT.length-1?tutEnd():tutGo(ti+1)}else if(a==='tback')tutGo(ti-1);else if(a==='tskip')tutEnd();
else if(a==='lefty'){save.lefty=!save.lefty;document.body.classList.toggle('lefty',save.lefty);persist();setScene(scene)}});
function toast_(t){const e=$('#ach');e.innerHTML=t;e.classList.remove('show');void e.offsetWidth;e.classList.add('show')}
function pause(){if(scene==='play')setScene('pause')}
// ---------- input ----------
const KM={w:0,arrowup:0,d:1,arrowright:1,s:2,arrowdown:2,a:3,arrowleft:3};
addEventListener('keydown',e=>{const k=e.key.toLowerCase();A.init();
if(k in KM||k===' '||k==='e')e.preventDefault();
if(k in KM)kd.add(KM[k]);if((k==='e'||k===' ')&&!e.repeat)actEdge=true;
if((k==='p'||k==='escape')&&!e.repeat){if(scene==='play')pause();else if(scene==='pause')setScene('play')}});
addEventListener('keyup',e=>{const k=e.key.toLowerCase();if(k in KM)kd.delete(KM[k])});
addEventListener('blur',()=>{kd.clear();pause()});document.addEventListener('visibilitychange',()=>{if(document.hidden)pause()});
if(matchMedia('(pointer:coarse)').matches||'ontouchstart' in window)document.body.classList.add('touch');
const joy=$('#joy'),knob=$('#knob');let jid=null,jc=[0,0];
function jmove(e){const L=joy.offsetWidth/2-8;let dx=e.clientX-jc[0],dy=e.clientY-jc[1];const m=Math.hypot(dx,dy);if(m>L){dx*=L/m;dy*=L/m}knob.style.transform='translate('+dx+'px,'+dy+'px)';jx=Math.abs(dx)/L>.15?dx/L:0;jy=Math.abs(dy)/L>.15?dy/L:0}
document.addEventListener('pointerdown',e=>{if(scene!=='play'||jid!==null||e.pointerType==='mouse'||e.target.closest('button')||e.clientY<78)return;
if(save.lefty?e.clientX<innerWidth*.38:e.clientX>innerWidth*.62)return;e.preventDefault();A.init();jid=e.pointerId;const R=joy.offsetWidth/2;jc=[e.clientX,e.clientY];
joy.style.cssText='left:'+(jc[0]-R)+'px;top:'+(jc[1]-R)+'px;bottom:auto;right:auto;opacity:1';jmove(e)});
document.addEventListener('pointermove',e=>{if(e.pointerId===jid)jmove(e)});
const jend=e=>{if(e.pointerId===jid){jid=null;jx=jy=0;knob.style.transform='';joy.style.cssText=''}};
document.addEventListener('pointerup',jend);document.addEventListener('pointercancel',jend);

$('#act').addEventListener('pointerdown',e=>{e.preventDefault();A.init();actEdge=true});
document.addEventListener('contextmenu',e=>e.preventDefault());
function resize(){const rt=document.body.classList.contains('touch')&&innerHeight>innerWidth?190:0;
const s=Math.max(.5,Math.min(innerWidth/VW,(innerHeight-72-rt)/VH));
cv.style.width=VW*s+'px';cv.style.height=VH*s+'px';$('#stage').style.paddingBottom=rt+'px'}
addEventListener('resize',resize);resize();
// ---------- level ----------
function loadLevel(i){const L=LEVELS[i];S={i,W:L.m[0].length,H:L.m.length,g:L.m.map(r=>r.split('')),movers:[],key:null,hasKey:false,cp:null,gate:false,plate:0,flip:false,
t:0,time:0,px:0,py:0,sx:0,sy:0,face:2,walk:0,stepT:0,chaos:null,next:6,lastType:'',ps:[],toast:{t:0,txt:''},shake:0,sp:{},msgT:0,why:'',att:1,gem:null,hid:false,si:0,fall:0,bs:{},moving:false,bgc:null,tor:[],coins:[],q:null,r:null,tp:0,dust:[],cx:0,cy:0,aut:0,lock:false};
for(let y=0;y<S.H;y++)for(let x=0;x<S.W;x++){const c=S.g[y][x],X=x*TS+16,Y=y*TS+16;
if(c==='P'){S.sx=S.px=X;S.sy=S.py=Y;S.g[y][x]='.'}
else if(c==='K'){S.key={x:X,y:Y,got:false};S.g[y][x]='.'}
else if(c==='G'){S.gem={x:X,y:Y,got:false};S.g[y][x]='.'}
else if(c==='M'||c==='N'){const f=L.ms||1,v=((x>>1)&1?1:-1)*50*f;S.movers.push({t:c==='M'?'h':'v',x:X,y:Y,sx:X,sy:Y,vx:c==='M'?50*f:0,vy:c==='N'?v:0,vx0:c==='M'?50*f:0,vy0:c==='N'?v:0});S.g[y][x]='.'}
else if(c==='Z'||c==='Y'){S.movers.push({t:'z',x:X,y:Y,sx:X,sy:Y,vx:0,vy:0,vx0:0,vy0:0,st:0,w:0,sp:(c==='Y'?78:55)*Math.min(1.3,L.ms||1)});S.g[y][x]='.'}
else if(c==='*'){S.coins.push({x:X,y:Y,got:false});S.g[y][x]='.'}
else if(c==='Q'){S.q=[X,Y]}else if(c==='R'){S.r=[X,Y]}}}
function respawn(){const c=S.cp;S.px=c?c.x:S.sx;S.py=c?c.y:S.sy;S.gate=c?c.gate:false;S.hasKey=c?c.hasKey:false;if(S.key)S.key.got=S.hasKey;
S.plate=0;S.chaos=null;S.next=5;S.flip=false;S.si=0;S.movers.forEach(m=>{m.x=m.sx;m.y=m.sy;m.vx=m.vx0;m.vy=m.vy0;m.st=0});S.sp={};S.toast.t=0;S.cx=Math.max(0,Math.min(S.W*TS-VW,S.px-VW/2));S.cy=0;S.tp=0}
const tile=(x,y)=>x<0||y<0||x>=S.W||y>=S.H?'#':S.g[y][x];
function solid(x,y){const c=tile(x,y);return c==='#'||c==='E'||(c==='D'&&!S.gate)||(c==='d'&&S.plate<=0)||(c==='W'&&!S.flip)||(c==='w'&&S.flip)||(c==='H'&&!S.hid)}
const over=(x,y)=>Math.abs(S.px-(x*TS+16))<25&&Math.abs(S.py-(y*TS+16))<25;
function hit(x,y){const x0=Math.floor((x-HB)/TS),x1=Math.floor((x+HB-.01)/TS),y0=Math.floor((y-HB)/TS),y1=Math.floor((y+HB-.01)/TS);
for(let ty=y0;ty<=y1;ty++)for(let tx=x0;tx<=x1;tx++)if(solid(tx,ty)){if('DdWw'.includes(tile(tx,ty))&&over(tx,ty))continue;return true}return false}
function move(dx,dy){if(dx){if(!hit(S.px+dx,S.py))S.px+=dx;else for(const o of[3,-3,6,-6])if(!hit(S.px+dx,S.py+o)){S.py+=o>0?.9:-.9;S.px+=dx;break}}
if(dy){if(!hit(S.px,S.py+dy))S.py+=dy;else for(const o of[3,-3,6,-6])if(!hit(S.px+o,S.py+dy)){S.px+=o>0?.9:-.9;S.py+=dy;break}}}
const ptx=()=>Math.floor(S.px/TS),pty=()=>Math.floor(S.py/TS);
function burst(x,y,c,n=14){for(let i=0;i<n;i++){const a=Math.random()*6.28,v=30+Math.random()*80;S.ps.push({x,y,vx:Math.cos(a)*v,vy:Math.sin(a)*v,l:.6+Math.random()*.4,c})}}
function die(why){if(scene!=='play')return;S.att++;A.die();burst(S.px,S.py,'#ff4d6d',30);S.shake=.5;S.why=why;$('#why').textContent=tr(why);
scene='dying';setTimeout(()=>{if(scene==='dying')setScene('over')},350)}
// ---------- chaos ----------
function nearHazard(){const tx=ptx(),ty=pty();for(let y=ty-1;y<=ty+1;y++)for(let x=tx-1;x<=tx+1;x++){const c=tile(x,y);if(c==='V'||c==='B'||c==='T')return true}return false}
function startChaos(){const L=LEVELS[S.i];let type;if(L.seq)type=L.seq[S.si%L.seq.length];else{let pool=L.c.filter(t=>t!==S.lastType);if(!pool.length)pool=L.c;type=pool[Math.random()*pool.length|0]}
if(['reverse','gravity','freeze'].includes(type)&&nearHazard()){S.next=.8;return}if(L.seq)S.si++;
if(['reverse','gravity','freeze'].includes(type)&&S.movers.some(m=>Math.hypot(m.x-S.px,m.y-S.py)<110)){S.next=.8;return}
const c={type,ph:'warn',t:0,dir:Math.random()*4|0,rule:Math.random()<.5?0:1,cand:[],bad:0,vt:0};
if(type==='invis'){for(let y=0;y<S.H;y++)for(let x=0;x<S.W;x++)if(S.g[y][x]==='.'&&Math.random()<.3)c.cand.push(x+','+y)}
S.chaos=c;S.lastType=type;A.warn()}
function activate(c){A.chaos();S.shake=.35;S.flash=.6;
if(c.type==='invis'){c.void=new Set(c.cand.filter(k=>{const[x,y]=k.split(',').map(Number);return Math.hypot(x*TS+16-S.px,y*TS+16-S.py)>56}))}
if(c.type==='walls'){S.flip=!S.flip;A.wall()}}
function endChaos(){save.chaosN++;achCheck();if(S.chaos.type==='walls'){S.flip=!S.flip;A.wall()}S.chaos=null;S.next=LEVELS[S.i].gap||4.5+Math.random()*3}
function updateChaos(dt){let c=S.chaos;if(!c){S.next-=dt;if(S.next<=0)startChaos();return}
c.t+=dt;if(c.ph==='warn'){if(c.t>=WARN){c.ph='on';c.t=0;activate(c)}return}
if(c.type==='invis'&&c.void.has(ptx()+','+pty())){c.vt+=dt;if(c.vt>.12)return die('d_floor')}else c.vt=0;
if(c.type==='color'){if(((ptx()+pty())&1)!==c.rule){c.bad+=dt;if(c.bad>.8)return die('d_color')}else c.bad=0}
if(c.t>=CH[c.type].d*(LEVELS[S.i].dm||1))endChaos()}
// ---------- update ----------
function update(dt){S.t+=dt;S.time+=dt;S.shake=Math.max(0,S.shake-dt);if(S.toast.t>0)S.toast.t-=dt;
updateChaos(dt);if(scene!=='play')return;
const c=S.chaos,on=c&&c.ph==='on';
let ix=(kd.has(1)?1:0)-(kd.has(3)?1:0)+jx,iy=(kd.has(2)?1:0)-(kd.has(0)?1:0)+jy;const m=Math.hypot(ix,iy);if(m>1){ix/=m;iy/=m}
if(on&&c.type==='reverse'){ix=-ix;iy=-iy}if(on&&c.type==='freeze')ix=iy=0;
let vx=ix*SPD,vy=iy*SPD;if(on&&c.type==='gravity'){vx+=DIRS[c.dir][0]*55;vy+=DIRS[c.dir][1]*55}
const px0=S.px,py0=S.py;move(vx*dt,vy*dt);
S.moving=m>.1;if(m>.1){S.walk+=dt*10;S.stepT-=dt;if(S.stepT<=0){A.step_();S.stepT=.28;S.ps.push({x:S.px+(Math.random()-.5)*6,y:S.py+8,vx:(Math.random()-.5)*14,vy:-8,l:.35,c:'#b8b0d0'})}
S.face=Math.abs(ix)>Math.abs(iy)?(ix>0?1:3):(iy>0?2:0)}
// movers
for(const o of S.movers){if(o.t==='z')monster(o,dt);else{o.x+=o.vx*dt;o.y+=o.vy*dt;if(solid(Math.floor((o.x+Math.sign(o.vx)*14)/TS),Math.floor((o.y+Math.sign(o.vy)*14)/TS))){o.vx=-o.vx;o.vy=-o.vy}}
if(Math.hypot(o.x-S.px,o.y-S.py)<(o.t==='z'?15:16))return die(o.t==='z'?'d_ghoul':'d_sweep')}
const tx=ptx(),ty=pty(),tc=tile(tx,ty);
// traps
for(let y=Math.max(0,ty-5);y<Math.min(S.H,ty+6);y++)for(let x=Math.max(0,tx-5);x<Math.min(S.W,tx+6);x++)if(S.g[y][x]==='T'){
const st=spike(x,y),k=x+','+y;if(st===2&&S.sp[k]!==2&&S.t-(S.lastTrap||0)>.12){A.trap();S.lastTrap=S.t}S.sp[k]=st;
if(st===2&&Math.abs(S.px-(x*TS+16))<12&&Math.abs(S.py-(y*TS+16))<12)return die('d_spike')}
if(tc==='V'||(tc==='B'&&bridge(tx,ty)===2)){S.fall+=dt;if(S.fall>.1)return die('d_fall')}else S.fall=0;
for(let y=Math.max(0,ty-3);y<Math.min(S.H,ty+4);y++)for(let x=Math.max(0,tx-3);x<Math.min(S.W,tx+4);x++)if(S.g[y][x]==='B'){const k=x+','+y,st=bridge(x,y);if(st===1&&S.bs[k]!==1&&S.t-(S.lastTick||0)>.2){A.tick();S.lastTick=S.t}S.bs[k]=st}
extras(dt,tx,ty,tc);
// plate / checkpoint / key / switch / exit
if(tc==='L'){if(S.plate<=0){A.click();toast(tr('plate'),1.5)}S.plate=8}else if(S.plate>0)S.plate-=dt;
if(tc==='C'&&(!S.cp||S.cp.x!==tx*TS+16||S.cp.y!==ty*TS+16)){S.cp={x:tx*TS+16,y:ty*TS+16,gate:S.gate,hasKey:S.hasKey};A.key();toast(tr('cp'),1.5);burst(S.px,S.py,'#6f6')}
if(S.key&&!S.key.got&&Math.hypot(S.key.x-S.px,S.key.y-S.py)<18){S.key.got=S.hasKey=true;A.key();burst(S.key.x,S.key.y,'#ffd23f');toast(tr('keyok'),2)}
gemCheck();if(actEdge){actEdge=false;tryHidden();for(let y=0;y<S.H;y++)for(let x=0;x<S.W;x++)if(S.g[y][x]==='S'&&Math.hypot(x*TS+16-S.px,y*TS+16-S.py)<34){
if(!S.gate){S.gate=true;A.sw();toast(tr('gateok'),2);burst(x*TS+16,y*TS+16,'#f90')}}}
for(let y=0;y<S.H;y++)for(let x=0;x<S.W;x++)if(S.g[y][x]==='E'&&Math.hypot(x*TS+16-S.px,y*TS+16-S.py)<30){
if(S.hasKey)return complete();if(S.t-S.msgT>2){S.msgT=S.t;toast(tr('locked_door'),1.5)}}}
function spike(x,y){const p=(S.t+x*.17+y*.31)%2.4;return p<1.4?0:p<1.8?1:2}
function complete(){if(roomCompletionCounted)return;roomCompletionCounted=true;if(window.CHAOS_ADS)CHAOS_ADS.roomCompleted();A.door();setTimeout(()=>A.win(),300);burst(S.px,S.py,'#7dff9a',40);const t=S.time,i=S.i,at=S.att,sec=S.gem&&S.gem.got;
const cg=S.coins.filter(c=>c.got).length,tb=Math.max(0,Math.round(600-t*4*15/S.W)),ap=Math.min(400,(at-1)*80),sb=(sec?250:0)+cg*40,sc=Math.max(300,1000+tb-ap+sb);let nb=false;
if(!save.best[i]||t<save.best[i]){save.best[i]=t;nb=true}if(!save.rs[i]||sc>save.rs[i])save.rs[i]=sc;
save.unlocked=Math.max(save.unlocked,Math.min(i+2,LEVELS.length));save.att[i]=Math.min(save.att[i]||99,at);persist();
if(i===0){ach('first');if(t<25)ach('speed')}if(at===1)ach('flawless');if(Object.keys(save.rs).length>=LEVELS.length)ach('all');
API.submit({name:save.name,pid:save.pid,room:i+1,timeMs:Math.round(t*1000),attempts:at,secret:!!sec,clientScore:sc,v:3});scene='dying';
setTimeout(()=>{const tot=totalScore();if(i===LEVELS.length-1){$('#vtxt').textContent=tr('sfinal',{s:sc,t:tot});setScene('victory')}
else{$('#dtxt').innerHTML=tr('stime',{t:fmt(t),b:tb})+'<br>'+tr('sattempts',{a:at,p:ap})+'<br>'+tr('ssecret')+': '+(sec?tr('sfound'):'—')+(S.coins.length?'<br>'+tr('scoins',{a:cg,b:S.coins.length,p:cg*40}):'')+'<br><b>'+tr('sroom')+' '+sc+'</b> · '+tr('stotal')+' '+tot+(nb?'<br>'+tr('snew'):'');setScene('done')}},700)}
const hash=(x,y)=>((x*73856093)^(y*19349663))>>>0;
const VIG=(()=>{const c=document.createElement('canvas');c.width=VW;c.height=VH;const g=c.getContext('2d'),v=g.createRadialGradient(VW/2,VH/2,120,VW/2,VH/2,330);v.addColorStop(0,'rgba(0,0,0,0)');v.addColorStop(1,'rgba(0,0,0,.7)');g.fillStyle=v;g.fillRect(0,0,VW,VH);return c})();
const bridge=(x,y)=>{const p=(S.t+x*.5+y*.2)%4;return p<2.8?0:p<3.4?1:2};
const PAL=[['#2b2543','#38305a','#14102a','#4a4075','#1c1932','#181530'],['#1f3a3f','#2c5258','#0e2024','#3c7078','#112a2e','#0f2427'],['#3d2230','#563046','#1c0e16','#7a4562','#261520','#20111a'],['#26402c','#35583d','#10200f','#4b7a54','#14281a','#112216'],['#2f4468','#41608f','#14203a','#5a85c0','#182640','#142038'],['#4a3c22','#675430','#201808','#8a7340','#2a2214','#241d10']];
const mhit=(x,y)=>{for(const[dx,dy]of[[-7,-7],[7,-7],[-7,7],[7,7]])if(solid(Math.floor((x+dx)/TS),Math.floor((y+dy)/TS)))return true;return false};
function monster(o,dt){const d=Math.hypot(o.x-S.px,o.y-S.py);
if(o.st===0){if(d<100){o.st=1;o.w=.7;A.tick()}}
else if(o.st===1){o.w-=dt;if(o.w<=0)o.st=2}
else{if(d>260){o.st=0;return}const sp=o.sp*dt,ax=S.px-o.x,ay=S.py-o.y,m=Math.hypot(ax,ay)||1,mx=ax/m*sp,my=ay/m*sp;
if(!mhit(o.x+mx,o.y))o.x+=mx;else if(Math.abs(ay)>2&&!mhit(o.x,o.y+Math.sign(ay)*sp))o.y+=Math.sign(ay)*sp;
if(!mhit(o.x,o.y+my))o.y+=my;else if(Math.abs(ax)>2&&!mhit(o.x+Math.sign(ax)*sp,o.y))o.x+=Math.sign(ax)*sp}}
function drawMonster(x,o,T){const st=o.st,fast=o.sp>60,X=Math.round(o.x),Y=Math.round(o.y+Math.sin(T*(st===2?10:3)+o.sx)*2);
x.fillStyle='rgba(0,0,0,.35)';x.fillRect(X-9,Math.round(o.y)+9,18,4);if(st===2){x.fillStyle='rgba(255,40,80,.15)';x.beginPath();x.arc(X,Y,22,0,7);x.fill()}
x.fillStyle=st===2?(fast?'#6a1a38':'#3a1a4a'):'#2a2238';x.fillRect(X-9,Y-10,18,18);x.fillStyle=st===2?(fast?'#8a2a50':'#5a2a70'):'#3a3050';x.fillRect(X-9,Y-10,18,5);
for(let k=-9;k<8;k+=6)x.fillRect(X+k,Y+8,4,3+(((T*6+k)|0)&1));
if(st===0){x.fillStyle='#777';x.fillRect(X-5,Y-3,4,1);x.fillRect(X+2,Y-3,4,1)}else{x.fillStyle='#ff2a4a';x.fillRect(X-5,Y-4,3,3);x.fillRect(X+2,Y-4,3,3)}
if(st===1){x.fillStyle='#ff4a6a';x.font='bold 12px monospace';x.fillText('!',X-3,Y-14)}}
function belt(x,X,Y,ch,T){x.fillStyle='rgba(90,160,220,.12)';x.fillRect(X,Y,TS,TS);x.strokeStyle='rgba(140,210,255,.65)';x.lineWidth=2;const o=(T*30)%16,fw=ch==='>'||ch==='v';
for(const k of[0,16]){const p=(k+(fw?o:16-o))%32;if(p>24)continue;x.beginPath();
if(ch==='>'){x.moveTo(X+p,Y+8);x.lineTo(X+p+8,Y+16);x.lineTo(X+p,Y+24)}else if(ch==='<'){x.moveTo(X+p+8,Y+8);x.lineTo(X+p,Y+16);x.lineTo(X+p+8,Y+24)}
else if(ch==='v'){x.moveTo(X+8,Y+p);x.lineTo(X+16,Y+p+8);x.lineTo(X+24,Y+p)}else{x.moveTo(X+8,Y+p+8);x.lineTo(X+16,Y+p);x.lineTo(X+24,Y+p+8)}x.stroke()}x.lineWidth=1}
function portal(x,X,Y,T){const s=Math.sin(T*5)*2;x.fillStyle='rgba(40,200,255,.18)';x.beginPath();x.arc(X+16,Y+16,13+s,0,7);x.fill();x.strokeStyle='#5ff';x.beginPath();x.arc(X+16,Y+16,10+s,0,7);x.stroke();x.strokeStyle='#bff';x.beginPath();x.arc(X+16,Y+16,5,T*4,T*4+4);x.stroke()}
function camStep(dt){const tx=Math.max(0,Math.min(S.W*TS-VW,S.px-VW/2)),ty=Math.max(0,Math.min(S.H*TS-VH,S.py-VH/2)),k=Math.min(1,dt*8);S.cx+=(tx-S.cx)*k;S.cy+=(ty-S.cy)*k}
function useSwitch(){for(let y=0;y<S.H;y++)for(let x=0;x<S.W;x++)if(S.g[y][x]==='S'&&!S.gate&&Math.hypot(x*TS+16-S.px,y*TS+16-S.py)<38){S.gate=true;A.sw();toast(tr('gateok'),2);burst(x*TS+16,y*TS+16,'#f90')}}
function extras(dt,tx,ty,tc){
if('><^v'.includes(tc)){const d={'>':[1,0],'<':[-1,0],'^':[0,-1],v:[0,1]}[tc];move(d[0]*62*dt,d[1]*62*dt)}
if(S.tp>0)S.tp-=dt;else if(S.q&&(tc==='Q'||tc==='R')){const t=tc==='Q'?S.r:S.q;if(t){burst(S.px,S.py,'#5ff',14);S.px=t[0];S.py=t[1];S.tp=1;A.mech();burst(S.px,S.py,'#5ff',14)}}
for(const c of S.coins)if(!c.got&&Math.hypot(c.x-S.px,c.y-S.py)<15){c.got=true;A.coin();burst(c.x,c.y,'#ffd23f',8)}
if(save.auto!==false){let near=false;for(let y=0;y<S.H;y++)for(let x=0;x<S.W;x++)if(S.g[y][x]==='S'&&!S.gate&&Math.hypot(x*TS+16-S.px,y*TS+16-S.py)<38)near=true;
if(near){S.aut+=dt;if(S.aut>.3&&!S.lock){S.lock=true;useSwitch()}}else{S.aut=0;S.lock=false}}
camStep(dt)}
const GLOW=(()=>{const c=document.createElement('canvas');c.width=c.height=128;const g=c.getContext('2d'),r=g.createRadialGradient(64,64,4,64,64,64);r.addColorStop(0,'rgba(255,200,120,.30)');r.addColorStop(.5,'rgba(150,120,255,.10)');r.addColorStop(1,'rgba(0,0,0,0)');g.fillStyle=r;g.fillRect(0,0,128,128);return c})();
const TINT=['rgba(80,40,160,.06)','rgba(20,140,150,.07)','rgba(160,30,60,.07)','rgba(40,140,70,.06)','rgba(60,110,200,.07)','rgba(190,150,50,.07)'];
const CHI={reverse:'reverse',gravity:'gravity',invis:'ghost',walls:'wall',color:'palette',freeze:'snow',dark:'moon'};
function decor(g,th,X,Y,h){const r=(h>>4)%12,q=(h>>8)%10;
if(th===0){g.fillStyle='#8a8498';g.fillRect(X+6+r,Y+8+q,6,2);g.fillRect(X+8+r,Y+10+q,2,4);g.fillRect(X+5+r,Y+13+q,3,2)}
else if(th===1){g.fillStyle='rgba(61,224,224,.25)';g.fillRect(X+5+r,Y+14+q,10,5);g.fillStyle='#3de0e0';g.fillRect(X+8+r,Y+10+q,2,7);g.fillRect(X+11+r,Y+13+q,2,4)}
else if(th===2){g.fillStyle='rgba(120,10,30,.5)';g.fillRect(X+6+r,Y+12+q,12,5);g.fillRect(X+9+r,Y+10+q,6,2);g.fillStyle='#b8b0a0';g.fillRect(X+10+r,Y+8+q,3,3)}
else if(th===3){g.fillStyle='#e04a4a';g.fillRect(X+8+r,Y+10+q,7,3);g.fillStyle='#d8d0b8';g.fillRect(X+10+r,Y+13+q,3,4)}
else if(th===4){g.fillStyle='rgba(191,232,255,.2)';g.fillRect(X+5+r,Y+15+q,11,3);g.fillStyle='#bfe8ff';g.fillRect(X+8+r,Y+8+q,2,9);g.fillRect(X+11+r,Y+11+q,2,6)}
else{g.fillStyle='#b8902f';g.fillRect(X+8+r,Y+10+q,7,7);g.fillStyle='#6a5018';g.fillRect(X+8+r,Y+10+q,7,2);g.fillStyle='#ffd23f';g.fillRect(X+10+r,Y+13+q,3,2)}}
function buildStatic(){const c=document.createElement('canvas');c.width=S.W*TS;c.height=S.H*TS;const g=c.getContext('2d'),W=(x,y)=>tile(x,y)==='#',th=LEVELS[S.i].th||0,P=PAL[th];S.tor=[];
for(let ty=0;ty<S.H;ty++)for(let tx=0;tx<S.W;tx++){const X=tx*TS,Y=ty*TS,h=hash(tx,ty);
if(W(tx,ty)){g.fillStyle=P[0];g.fillRect(X,Y,TS,TS);const o=(ty&1)?8:20;
g.fillStyle='rgba(255,255,255,.04)';g.fillRect(X,Y+3,o,12);g.fillStyle='rgba(0,0,0,.08)';g.fillRect(X+o,Y+3,TS-o,12);g.fillStyle='rgba(255,255,255,.03)';g.fillRect(X+(28-o),Y+17,TS-(28-o),14);
g.fillStyle=P[2];g.fillRect(X,Y+15,TS,2);g.fillRect(X+o,Y,2,15);g.fillRect(X+(28-o),Y+17,2,15);
if(!W(tx,ty-1)){g.fillStyle=P[3];g.fillRect(X,Y,TS,2);g.fillStyle=P[1];g.fillRect(X,Y+2,TS,2)}else{g.fillStyle=P[1];g.fillRect(X,Y,TS,1)}
if(!W(tx-1,ty)){g.fillStyle=P[1];g.fillRect(X,Y,2,TS)}if(!W(tx+1,ty)){g.fillStyle='rgba(0,0,0,.3)';g.fillRect(X+TS-3,Y,3,TS)}
if(h%7===0){g.fillStyle='rgba(255,255,255,.05)';g.fillRect(X+(h>>3)%18+3,Y+(h>>6)%8+4,6,2)}
if(ty<S.H-1&&!W(tx,ty+1)){g.fillStyle=P[2];g.fillRect(X,Y+TS-9,TS,9);g.fillStyle=P[3];g.fillRect(X,Y+TS-10,TS,1);g.fillStyle='rgba(0,0,0,.35)';g.fillRect(X,Y+TS-3,TS,3);if(h%11===0&&ty>0)S.tor.push([X+16,Y+20,h])}}
else{g.fillStyle=(tx+ty)&1?P[4]:P[5];g.fillRect(X,Y,TS,TS);
g.fillStyle='rgba(255,255,255,.04)';g.fillRect(X,Y,TS,1);g.fillRect(X,Y,1,TS);g.fillStyle='rgba(0,0,0,.18)';g.fillRect(X,Y+TS-1,TS,1);g.fillRect(X+TS-1,Y,1,TS);
g.fillStyle='rgba(255,255,255,.06)';for(let k=0;k<3;k++)g.fillRect(X+((h>>(k*3))%28)+2,Y+((h>>(k*3+1))%28)+2,2,2);
if(h%17===0){g.fillStyle='rgba(0,0,0,.3)';g.fillRect(X+8,Y+10,2,12);g.fillRect(X+10,Y+16,7,2);g.fillRect(X+16,Y+16,2,8)}
if(h%31===7){g.fillStyle='rgba(106,90,205,.15)';g.fillRect(X+12,Y+10,8,8);g.fillStyle='rgba(138,122,237,.25)';g.fillRect(X+14,Y+12,4,4)}
if(h%9===2&&S.g[ty][tx]==='.'&&!W(tx,ty-1)&&!W(tx-1,ty))decor(g,th,X,Y,h);
if(W(tx,ty-1)){const gr=g.createLinearGradient(0,Y,0,Y+16);gr.addColorStop(0,'rgba(0,0,0,.5)');gr.addColorStop(1,'rgba(0,0,0,0)');g.fillStyle=gr;g.fillRect(X,Y,TS,16)}
if(W(tx-1,ty)){const gr=g.createLinearGradient(X,0,X+9,0);gr.addColorStop(0,'rgba(0,0,0,.35)');gr.addColorStop(1,'rgba(0,0,0,0)');g.fillStyle=gr;g.fillRect(X,Y,9,TS)}}}
g.globalCompositeOperation='lighter';for(const l of S.tor){const r=g.createRadialGradient(l[0],l[1]+8,2,l[0],l[1]+8,60);r.addColorStop(0,'rgba(255,170,80,.16)');r.addColorStop(1,'rgba(255,170,80,0)');g.fillStyle=r;g.fillRect(l[0]-60,l[1]-52,120,120)}g.globalCompositeOperation='source-over';
S.dust=Array.from({length:30},()=>({x:Math.random()*S.W*TS,y:Math.random()*S.H*TS,vx:(Math.random()-.5)*10,vy:-4-Math.random()*6}));S.bgc=c}
function pit(x,X,Y,T,tx,ty){x.fillStyle='#04020a';x.fillRect(X,Y,TS,TS);x.fillStyle='#10081e';x.fillRect(X+3,Y+3,TS-6,TS-6);x.fillStyle='#2a1450';x.fillRect(X+4+((T*9+tx*7+ty*3)|0)%20,Y+6+((tx*5+ty*11)%16),6,2);x.fillStyle='#3a2a6a';x.fillRect(X,Y,TS,2)}
function drawPlayer(x,bad){const px=Math.round(S.px),sy=Math.round(S.py),w=S.walk,mv=S.moving,f=S.face,bob=mv?-Math.round(Math.abs(Math.sin(w))*2):Math.round(Math.sin(S.t*2.2)*.7),py=sy+bob,lg=mv?(Math.sin(w)>0?1:-1):0,sw=mv?Math.round(Math.sin(w)*2):Math.round(Math.sin(S.t*3));
x.fillStyle='rgba(0,0,0,.4)';x.fillRect(px-8,sy+8,16,4);x.fillRect(px-6,sy+7,12,1);
const cl=bad?'#f44':'#5a4ac0',parts=[[px-5,py+7-(lg>0?1:0),4,4,'#2e2468'],[px+1,py+7-(lg<0?1:0),4,4,'#2e2468'],[px-7,py-5,14,13,cl],[px-6,py-14,12,10,'#4a3aa0']];
x.fillStyle='#0b0818';for(const p of parts)x.fillRect(p[0]-1,p[1]-1,p[2]+2,p[3]+2);
for(const p of parts){x.fillStyle=p[4];x.fillRect(p[0],p[1],p[2],p[3])}
x.fillStyle='#7a6ae0';x.fillRect(px-7,py-5,14,3);x.fillStyle='#3e3290';x.fillRect(px-7+(f===1?9:0),py-2,5,10);x.fillStyle='#8a7aed';x.fillRect(px-6,py-14,12,2);
x.fillStyle='#e8528a';x.fillRect(px-6,py-4,12,2);if(f===1||f===3){x.fillRect(px+(f===1?-9:7),py-3+sw,3,5);x.fillRect(px+(f===1?-11:10),py-2+sw*2,2,3)}
if(f!==0){x.fillStyle='#f0dcc4';x.fillRect(px-4+(f===1?2:f===3?-2:0),py-12,8,6);if(S.t%3.2>.15){x.fillStyle='#222';const ex=f===2?[-3,1]:f===1?[2]:[-4];for(const e of ex)x.fillRect(px+e,py-10,2,2)}}
const lx=px+(f===3?-11:f===1?10:9);x.fillStyle='#5a4a30';x.fillRect(lx,py-1+sw,3,2);x.fillStyle='#ffd27a';x.fillRect(lx,py+1+sw,3,4);x.fillStyle='rgba(255,210,120,.3)';x.fillRect(lx-2,py+sw,7,8)}
function render(){const x=cx,c=S.chaos,on=c&&c.ph==='on'?c:null,wr=c&&c.ph==='warn'?c:null,fl=Math.sin(S.t*20)>0,T=S.t,ov=c&&(c.type==='color'||c.type==='invis');
x.save();if(S.shake>0)x.translate(Math.round((Math.random()-.5)*S.shake*12),Math.round((Math.random()-.5)*S.shake*12));
x.save();x.translate(-Math.round(S.cx),-Math.round(S.cy));x.drawImage(S.bgc,0,0);const c0=Math.max(0,(S.cx/TS|0)-1),c1=Math.min(S.W,c0+18);
for(let ty=0;ty<S.H;ty++)for(let tx=c0;tx<c1;tx++){const ch=S.g[ty][tx];if(ch==='#'||(ch==='.'&&!ov))continue;const X=tx*TS,Y=ty*TS;
if(c&&c.type==='color'&&(wr||on)){x.globalAlpha=on?.8:(fl?.45:.15);x.fillStyle=((tx+ty)&1)?'#27408a':'#8a2740';x.fillRect(X,Y,TS,TS);x.globalAlpha=1}
if(c&&c.type==='invis'){const k=tx+','+ty;if(wr&&c.cand.includes(k)){x.fillStyle=fl?'rgba(183,122,255,.35)':'rgba(0,0,0,.5)';x.fillRect(X,Y,TS,TS);x.strokeStyle='#b77aff';x.strokeRect(X+2,Y+2,TS-4,TS-4)}else if(on&&c.void.has(k)){x.fillStyle='#000';x.fillRect(X,Y,TS,TS);x.fillStyle='#220a3a';x.fillRect(X,Y,TS,3)}}
if(ch==='.')continue;
if(ch==='H'&&!S.hid){x.fillStyle='#2b2543';x.fillRect(X,Y,TS,TS);x.fillStyle='#38305a';x.fillRect(X,Y,TS,3);x.fillStyle='#14102a';x.fillRect(X,Y+TS-8,TS,8);x.fillStyle='rgba(0,0,0,.7)';x.fillRect(X+10,Y+6,2,10);x.fillRect(X+12,Y+14,7,2);x.fillRect(X+18,Y+14,2,10)}
else if(ch==='W'||ch==='w'){const sol=ch==='W'?!S.flip:S.flip;if(sol){x.fillStyle='#6a2a98';x.fillRect(X+1,Y+1,TS-2,TS-2);x.fillStyle='#a060e0';x.fillRect(X+1,Y+1,TS-2,4);x.fillStyle='rgba(224,176,255,.35)';x.fillRect(X+3,Y+4+((T*18+tx*5)|0)%22,TS-6,2);x.strokeStyle='#e0b0ff';x.strokeRect(X+2,Y+2,TS-4,TS-4)}
else{x.strokeStyle=(wr&&wr.type==='walls'&&fl)?'#fff':'rgba(122,58,168,.45)';x.setLineDash([4,4]);x.strokeRect(X+2,Y+2,TS-4,TS-4);x.setLineDash([])}}
else if(ch==='D'||ch==='d'){const op=ch==='D'?S.gate:S.plate>0,rg=ch==='D'?'255,153,0':'51,221,255';x.fillStyle='rgba('+rg+','+(op?.06:.18+.06*Math.sin(T*6))+')';x.fillRect(X,Y,TS,TS);x.fillStyle='rgb('+rg+')';x.globalAlpha=op?.25:1;for(let b=0;b<4;b++)x.fillRect(X+3+b*8,Y,4,TS);x.globalAlpha=1;if(!op){x.fillStyle='rgba(255,255,255,.55)';x.fillRect(X+3+((T*30)|0)%24,Y+((T*40)|0)%28,3,3)}}
else if(ch==='S'||ch==='X'){x.fillStyle='#2a2540';x.fillRect(X+5,Y+18,22,9);x.fillStyle='#14102a';x.fillRect(X+5,Y+25,22,2);const s2=ch==='S'?S.gate:S.flip;x.fillStyle=ch==='S'?(s2?'#5f5':'#f90'):'#c6f';
if(ch==='S'){x.fillRect(X+14,s2?Y+9:Y+5,4,15);x.fillRect(X+10,s2?Y+7:Y+3,12,5)}else{x.fillRect(X+14+(s2?5:-5),Y+7,4,14);x.fillRect(X+(s2?16:6),Y+4,10,5)}}
else if(ch==='L'){x.fillStyle='#2a2540';x.fillRect(X+3,Y+3,26,26);const p=S.plate>0;x.fillStyle=p?'#3df':'#146';x.fillRect(X+6,Y+(p?8:6),20,p?16:18);if(p){x.fillStyle='rgba(51,221,255,.2)';x.fillRect(X,Y,TS,TS)}}
else if(ch==='C'){const on3=S.cp&&S.cp.x===X+16&&S.cp.y===Y+16;x.fillStyle='#555';x.fillRect(X+10,Y+5,3,23);x.fillStyle='#333';x.fillRect(X+7,Y+26,9,3);x.fillStyle=on3?'#5f5':'#aa5';for(let q=0;q<13;q+=2)x.fillRect(X+13+q,Y+6+Math.round(Math.sin(T*6+q*.6)*1.2),2,9-(q/3|0))}
else if(ch==='T'){const st=spike(tx,ty);x.fillStyle='#1a1630';x.fillRect(X+3,Y+3,26,26);const hh=st===2?12:st===1?6:2;x.fillStyle=st===2?'#ff3355':st===1?'#ffaa33':'#6a6a80';for(let a=0;a<3;a++)for(let b=0;b<3;b++){const sx=X+6+a*8,sy=Y+6+b*8;x.fillRect(sx,sy+8-hh,4,hh);x.fillRect(sx+1,sy+7-hh,2,2)}if(st===2){x.fillStyle='rgba(255,50,80,.22)';x.fillRect(X,Y,TS,TS)}}
else if(ch==='E'){x.fillStyle='#3a3020';x.fillRect(X,Y,TS,TS);x.fillStyle=S.hasKey?'#3a8a4a':'#8a2a2a';x.fillRect(X+4,Y+3,24,26);x.fillStyle='#14102a';x.fillRect(X+8,Y+7,16,22);x.fillStyle=S.hasKey?'#8fa':'#f77';x.fillRect(X+14,Y+14,4,6);x.fillRect(X+15,Y+11,2,3);if(S.hasKey){x.fillStyle='rgba(120,255,160,'+(.12+.1*Math.sin(T*4))+')';x.fillRect(X-4,Y-4,TS+8,TS+8)}}
else if('><^v'.includes(ch))belt(x,X,Y,ch,T);else if(ch==='Q'||ch==='R')portal(x,X,Y,T);
else if(ch==='V'||(ch==='B'&&bridge(tx,ty)===2))pit(x,X,Y,T,tx,ty);
else if(ch==='B'){const st=bridge(tx,ty);if(st===1&&fl)pit(x,X,Y,T,tx,ty);else{x.fillStyle='#4a3520';x.fillRect(X,Y+2,TS,TS-4);x.fillStyle='#8a6a3a';for(let p=0;p<4;p++)x.fillRect(X+1,Y+4+p*7,TS-2,5);x.fillStyle=st?'rgba(255,170,60,.4)':'rgba(120,255,160,.22)';x.fillRect(X,Y,TS,TS)}}}
for(const l of S.tor){const f2=Math.sin(T*9+l[2]);if(Math.random()<.05)S.ps.push({x:l[0]+(Math.random()-.5)*4,y:l[1]-8,vx:(Math.random()-.5)*10,vy:-26-Math.random()*14,l:.7,c:'#ffb050'});x.fillStyle='#5a3a1a';x.fillRect(l[0]-2,l[1]-2,4,6);x.fillStyle='#ff9a3c';x.fillRect(l[0]-2,l[1]-6-f2,4,5);x.fillStyle='#ffe08a';x.fillRect(l[0]-1,l[1]-5-f2,2,3);x.fillStyle='rgba(255,160,70,.05)';x.beginPath();x.arc(l[0],l[1],40+f2*3,0,7);x.fill();x.beginPath();x.arc(l[0],l[1],22,0,7);x.fill()}
if(S.key&&!S.key.got){const k=S.key,by=Math.sin(T*4)*3;x.fillStyle='rgba(255,210,63,.18)';x.fillRect(k.x-12,k.y-12+by,24,24);x.fillStyle='#ffd23f';x.fillRect(k.x-6,k.y-8+by,8,8);x.fillStyle='#b8901f';x.fillRect(k.x-4,k.y-6+by,4,4);x.fillStyle='#ffd23f';x.fillRect(k.x-1,k.y-1+by,3,12);x.fillRect(k.x-1,k.y+7+by,6,2);if(Math.sin(T*7)>.6){x.fillStyle='#fff';x.fillRect(k.x+5,k.y-9+by,2,2)}}
if(S.gem&&!S.gem.got){const g=S.gem,by=Math.sin(T*3)*2;x.fillStyle='#5ff';x.beginPath();x.moveTo(g.x,g.y-9+by);x.lineTo(g.x+7,g.y+by);x.lineTo(g.x,g.y+9+by);x.lineTo(g.x-7,g.y+by);x.fill();x.fillStyle='rgba(255,255,255,.6)';x.fillRect(g.x-2,g.y-5+by,2,4)}
for(const c2 of S.coins)if(!c2.got){const w=Math.abs(Math.cos(T*4+c2.x))*5+1,b=Math.sin(T*3+c2.y)*2;x.fillStyle='#b8901f';x.fillRect(c2.x-w,c2.y-5+b,w*2,10);x.fillStyle='#ffd23f';x.fillRect(c2.x-w+1,c2.y-4+b,Math.max(1,w*2-2),8)}
for(const d of S.dust){d.x=(d.x+d.vx*.016+S.W*TS)%(S.W*TS);d.y=(d.y+d.vy*.016+S.H*TS)%(S.H*TS);x.fillStyle='rgba(200,190,255,'+(.15+.1*Math.sin(T*2+d.x))+')';x.fillRect(d.x|0,d.y|0,2,2)}
for(const o of S.movers){if(o.t==='z'){drawMonster(x,o,T);continue}x.fillStyle='rgba(0,0,0,.3)';x.fillRect(o.x-9,o.y+8,18,4);x.fillStyle='rgba(255,50,80,.18)';x.beginPath();x.arc(o.x,o.y,17+Math.sin(T*6)*1.5,0,7);x.fill();x.fillStyle='#b01828';x.fillRect(o.x-9,o.y-9,18,18);x.fillStyle='#e03048';x.fillRect(o.x-9,o.y-9,18,4);x.fillStyle='#f66';
for(let a=0;a<6;a++){const an=T*3+a*1.047;x.fillRect(Math.round(o.x+Math.cos(an)*12)-2,Math.round(o.y+Math.sin(an)*12)-2,4,4)}x.fillStyle='#fff';x.fillRect(o.x-5,o.y-3,3,3);x.fillRect(o.x+2,o.y-3,3,3);x.fillStyle='#000';const dd=o.vx>0?1:0;x.fillRect(o.x-4+dd,o.y-2,1,2);x.fillRect(o.x+3+dd,o.y-2,1,2)}
for(let y=0;y<S.H;y++)for(let xx=0;xx<S.W;xx++){const ch=S.g[y][xx];if((ch==='S'&&!S.gate)||ch==='X'||(ch==='H'&&!S.hid)){const X=xx*TS+16,Y=y*TS+16,d=Math.hypot(X-S.px,Y-S.py);
if(ch!=='H'||d<90){x.strokeStyle=d<40?'#fff':'rgba(255,210,63,.5)';x.beginPath();x.arc(X,Y,14+Math.sin(T*5)*2,0,7);x.stroke();if(d<40){x.fillStyle='rgba(0,0,0,.8)';x.fillRect(X-8,Y-30,16,14);x.fillStyle='#fff';x.font='bold 10px monospace';x.fillText('E',X-3,Y-19)}}}}
for(let i=S.ps.length-1;i>=0;i--){const p=S.ps[i];p.l-=.016;p.x+=p.vx*.016;p.y+=p.vy*.016;if(p.l<=0){S.ps.splice(i,1);continue}x.globalAlpha=Math.min(1,p.l*2);x.fillStyle=p.c;x.fillRect(Math.round(p.x),Math.round(p.y),3,3)}x.globalAlpha=1;
if(scene!=='over'){x.globalCompositeOperation='lighter';x.drawImage(GLOW,Math.round(S.px)-64,Math.round(S.py)-64);x.globalCompositeOperation='source-over';drawPlayer(x,on&&c.type==='color'&&c.bad>0&&fl);
if(on&&c.type==='gravity'){x.fillStyle='#6cf';const d=DIRS[c.dir];x.fillRect(S.px+d[0]*16-2,S.py+d[1]*16-2,4,4);x.fillRect(S.px+d[0]*22-2,S.py+d[1]*22-2,4,4)}}
x.restore();
if(on){const ty=on.type;if(ty==='gravity'){const d=DIRS[on.dir];x.fillStyle='rgba(136,221,255,.5)';for(let i=0;i<10;i++){const ox=(((i*97)+d[0]*T*80)%VW+VW)%VW,oy=(((i*53)+d[1]*T*80)%VH+VH)%VH;x.fillRect(ox,oy,d[0]?12:2,d[1]?12:2)}}
else if(ty==='freeze'){x.fillStyle='rgba(180,235,255,.12)';x.fillRect(0,0,VW,VH);x.strokeStyle='rgba(220,245,255,.4)';x.lineWidth=8;x.strokeRect(4,4,VW-8,VH-8);x.lineWidth=1}
else if(ty==='reverse'){x.fillStyle='rgba(255,92,138,.07)';for(let y=((T*40)%6);y<VH;y+=6)x.fillRect(0,y,VW,1)}}
if(c&&c.type==='dark'){const a=on?Math.min(1,c.t*2):.35*(fl?1:.5);dx.globalCompositeOperation='source-over';dx.clearRect(0,0,VW,VH);dx.fillStyle='#000';dx.fillRect(0,0,VW,VH);
dx.globalCompositeOperation='destination-out';const g=dx.createRadialGradient(S.px-S.cx,S.py-S.cy,20,S.px-S.cx,S.py-S.cy,90);g.addColorStop(0,'rgba(0,0,0,1)');g.addColorStop(1,'rgba(0,0,0,0)');dx.fillStyle=g;dx.fillRect(0,0,VW,VH);
x.globalAlpha=on?a*.97:a;x.drawImage(dk,0,0);x.globalAlpha=1}
x.fillStyle=TINT[LEVELS[S.i].th||0];x.fillRect(0,0,VW,VH);if(S.flash>0){x.fillStyle='rgba(255,255,255,'+(S.flash*.5)+')';x.fillRect(0,0,VW,VH);S.flash-=.03}x.drawImage(VIG,0,0);
if(wr&&fl){x.strokeStyle=CH[wr.type].c;x.lineWidth=6;x.strokeRect(3,3,VW-6,VH-6);x.lineWidth=1}
x.restore();hud()}
function hud(){const c=S.chaos,L=LEVELS[S.i],st=(e,t)=>{if(e._t!==t){e._t=t;e.textContent=t}};st($('#hlv'),tr('roomof',{a:S.i+1,b:LEVELS.length}));st($('#ht .t'),fmt(S.time));
const k=$('#hkey');st($('#hkey .t'),tr(S.hasKey?'keyyes':'keyno'));k.className=S.hasKey?'got':'';const hc=$('#hcoin');hc.style.display=S.coins.length?'':'none';st($('#hcoin .t'),S.coins.filter(q=>q.got).length+'/'+S.coins.length);
const ch=$('#chaos'),cn=$('#cn'),n=$('#cn .t'),s=$('#cs'),b=$('#cbi'),ci=$('#cni');ch.className=c?c.ph:'calm';
if(!c){setIc(ci,'check');st(n,tr('calm'));cn.style.color='#8fd';const q=Math.max(0,S.next).toFixed(0);st(s,L.seq?tr('nextnamed',{name:tr('ch_'+L.seq[S.si%L.seq.length]),n:q}):tr('nextin',{n:q}));b.style.width=Math.max(0,100-S.next*14)+'%';b.style.background='#5ca'}
else{const d=CH[c.type];cn.style.color=d.c;b.style.background=d.c;
if(c.ph==='warn'){setIc(ci,'warn');st(n,tr('warn',{name:tr('ch_'+c.type)}));st(s,tr('incoming',{n:(WARN-c.t).toFixed(1)}));b.style.width=(c.t/WARN*100)+'%'}
else{setIc(ci,CHI[c.type]);st(n,tr('ch_'+c.type));st(s,tr('cht_'+c.type,{d:tr('dir'+c.dir),c:tr(c.rule?'blue':'red')}));b.style.width=(100-c.t/(d.d*(L.dm||1))*100)+'%'}}
const t=$('#toast');st(t,S.toast.txt);t.style.opacity=S.toast.t>0?1:0}
function frame(t){const dt=Math.min(.05,(t-last)/1000||0);last=t;
if(S&&(scene==='play'||scene==='dying'))update(dt);if(S&&!MENU.has(scene))render();if(MENU.has(scene))drawBg(t);requestAnimationFrame(frame)}
const MENU=new Set(['menu','levels','settings','lb','how','tut']);
const ACHK=['first','flawless','speed','secret','chaos','all'];
const totalScore=()=>Object.values(save.rs).reduce((a,b)=>a+b,0);
function ach(k){if(save.ach[k])return;save.ach[k]=1;persist();A.ach();toast_(ico('trophy',22)+'<b>'+tr('a_'+k)+'</b><br><small>'+tr('ad_'+k)+'</small>')}
function achCheck(){if(save.chaosN>=15)ach('chaos')}
function gemCheck(){const g=S.gem;if(g&&!g.got&&Math.hypot(g.x-S.px,g.y-S.py)<18){g.got=true;save.sec[S.i]=1;persist();A.solved();burst(g.x,g.y,'#5ff',20);toast(tr('gem'),2.5);ach('secret')}}
function tryHidden(){for(let y=0;y<S.H;y++)for(let x=0;x<S.W;x++){const ch=S.g[y][x];if(Math.hypot(x*TS+16-S.px,y*TS+16-S.py)>=42)continue;
if(ch==='H'&&!S.hid){S.hid=true;A.sw();toast(tr('hid'),2.5);burst(x*TS+16,y*TS+16,'#bbb',20);return}
if(ch==='X'){S.flip=!S.flip;A.wall();toast(tr('xshift'),2);burst(x*TS+16,y*TS+16,'#c6f',16);return}}}
const TUT=[['move','mv'],['search','pl'],['key','pl'],['bolt','sp'],['spikes','sh'],['door','pl']];
let ti=0;
function tutGo(n){ti=Math.max(0,Math.min(TUT.length-1,n));const t=TUT[ti];$('#tic').innerHTML=ico(t[0],72);$('#tic').className='an-'+t[1];$('#tti').textContent=tr('tt'+(ti+1));$('#ttx').textContent=tr('tx'+(ti+1));
$('#tdots').textContent=TUT.map((_,i)=>i===ti?'●':'○').join(' ');$('#tnb').textContent=tr(ti===TUT.length-1?'tstart':'tnext');$('#tback').style.visibility=ti?'visible':'hidden'}
function tutEnd(){save.seen=$('#tck').checked;persist();setScene('menu')}
async function buildLb(){const t=$('#lbl'),pl=Object.keys(save.rs).length;$('#lbn').value=save.name;
$('#lbme').textContent=tr('lbtotal',{s:totalScore(),r:pl,n:LEVELS.length});
$('#lba').innerHTML=ACHK.map(k=>(save.ach[k]?ico('trophy',14):ico('lock',14))+tr('a_'+k)+' — '+tr('ad_'+k)).join('<br>');
if(!API.configured()){t.innerHTML='<div class="note">'+ico('globe',28)+'<br><span></span></div>';t.querySelector('span').textContent=tr('lbnone');return}
t.textContent=tr('lbload');try{const r=await API.top();t.innerHTML='<table>'+(r.players||[]).map((p,i)=>`<tr><td>${i+1}</td><td>${String(p.name).replace(/[<>&]/g,'')}</td><td>${+p.score||0}</td><td>${+p.rooms||0}</td><td>${(+p.bestTime||0).toFixed(1)}</td></tr>`).join('')+'</table>'+(r.me?'<p>'+tr('lbrank',{r:+r.me.rank||0})+'</p>':'')}catch(e){t.textContent=tr('lbfail')}}
$('#lbn').onchange=e=>{save.name=e.target.value.trim().slice(0,16);persist()};
$('#vm').oninput=e=>{save.mv=e.target.value/100;A.apply();persist()};$('#vs').oninput=e=>{save.sv=e.target.value/100;A.apply();persist();A.click()};
document.addEventListener('touchmove',e=>{if(scene==='play')e.preventDefault()},{passive:false});
const bg=$('#bg'),bx=bg.getContext('2d'),motes=Array.from({length:40},()=>({x:Math.random(),y:Math.random(),s:.02+Math.random()*.05,r:1+Math.random()*2}));
function drawBg(t){const w=innerWidth>>1,h=innerHeight>>1;if(bg.width!==w||bg.height!==h){bg.width=w;bg.height=h}
const g=bx.createLinearGradient(0,0,0,h);g.addColorStop(0,'#0b0618');g.addColorStop(1,'#1a0d33');bx.fillStyle=g;bx.fillRect(0,0,w,h);
for(let i=0;i<2;i++){const cx2=w*(.3+.4*i)+Math.sin(t/3000+i*3)*w*.15,cy2=h*.5+Math.cos(t/4000+i)*h*.2,r=bx.createRadialGradient(cx2,cy2,0,cx2,cy2,w*.5);r.addColorStop(0,i?'#3a1a7a55':'#7a1a5a33');r.addColorStop(1,'#0000');bx.fillStyle=r;bx.fillRect(0,0,w,h)}
bx.fillStyle='#c9b8ff';for(const m of motes){m.y-=m.s*.016;if(m.y<0)m.y=1;bx.globalAlpha=.25+.25*Math.sin(t/700+m.x*9);bx.fillRect(m.x*w+Math.sin(t/2000+m.y*6)*6,m.y*h,m.r,m.r)}bx.globalAlpha=1}
applyIcons();setScene(save.seen?'menu':'tut');requestAnimationFrame(frame);
