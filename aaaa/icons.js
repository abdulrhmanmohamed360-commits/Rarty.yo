// Pixel-art icons (10x10) drawn from text bitmaps; no emoji anywhere in the UI.
const ICONS={key:['a#ffcc33,b#a8760f,c#fff0a0','...aaaa...|..abccba..|..ab..ba..|..abccba..|...aaaa...|....aa....|....aa....|....aaaa..|....aa....|....aaaa..'],
coin:['a#8a6a10,b#e0a820,c#ffd23f,d#b8861a','...aaaa...|..abccba..|.abccccba.|.abcddcba.|.abcddcba.|.abccccba.|.abccccba.|..abccba..|...aaaa...|..........'],
clock:['a#cfc8ff,b#2a2250,c#ffffff','...aaaa...|..abbbba..|.abbbcbba.|.abbbcbba.|.abbbccba.|.abbbbbba.|.abbbbbba.|..abbbba..|...aaaa...|..........'],
warn:['a#ffb020,d#3a1a00','....aa....|...aaaa...|..aaddaa..|..aaddaa..|.aaaddaaa.|.aaaddaaa.|aaaaaaaaaa|aaaaddaaaa|aaaaaaaaaa|..........'],
check:['a#5fe08a','..........|.........a|........aa|.......aa.|a.....aa..|aa...aa...|.aa.aa....|..aaa.....|...a......|..........'],
lock:['a#c8c0e8,b#8a7a30,c#d8b840,d#3a2a00','...aaaa...|..a....a..|..a....a..|.bbbbbbbb.|.bccccccb.|.bccddccb.|.bccddccb.|.bccccccb.|.bbbbbbbb.|..........'],
play:['a#ffffff','aa........|aaaa......|aaaaaa....|aaaaaaaa..|aaaaaaaaaa|aaaaaaaaaa|aaaaaaaa..|aaaaaa....|aaaa......|aa........'],
pause:['a#ffffff','..........|.aaa..aaa.|.aaa..aaa.|.aaa..aaa.|.aaa..aaa.|.aaa..aaa.|.aaa..aaa.|.aaa..aaa.|.aaa..aaa.|..........'],
trophy:['a#e0a820,b#ffd23f','.aaaaaaaa.|aabbbbbbaa|a.abbbba.a|a.abbbba.a|.aabbbbaa.|...abba...|....bb....|....bb....|...aaaa...|..aaaaaa..'],
globe:['a#6ac8ff,b#1a4a7a,c#5fe08a','...aaaa...|..abbcba..|.abccbbba.|.abccbbba.|.abbbbcca.|.abbbbcca.|.abbccbba.|..abbbba..|...aaaa...|..........'],
reverse:['a#ff5c8a','...a......|..aa......|.aaaaaaaa.|..aa......|...a......|......a...|......aa..|.aaaaaaaa.|......aa..|......a...'],
gravity:['a#6ccfff','....aa....|....aa....|....aa....|....aa....|.aaaaaaaa.|..aaaaaa..|...aaaa...|....aa....|..........|.aaaaaaaa.'],
bolt:['a#ffd23f','.....aa...|....aa....|...aa.....|..aaaaaa..|....aa....|...aa.....|..aa......|.aa.......|.a........|..........'],
ghost:['a#d8d0ff,b#2a2250','...aaaa...|..aaaaaa..|.aaaaaaaa.|.aabaabaa.|.aabaabaa.|.aaaaaaaa.|.aaaaaaaa.|.aaaaaaaa.|.aa.aa.aa.|.a..a..a..'],
wall:['a#a060e0,b#4a1a70','bbbbbbbbbb|aaaabaaaaa|aaaabaaaaa|bbbbbbbbbb|aaabaaaaaa|aaabaaaaaa|bbbbbbbbbb|aaaaabaaaa|aaaaabaaaa|bbbbbbbbbb'],
palette:['a#e04a6a,b#4a7ae0,c#ffd23f','aaaa..bbbb|aaaa..bbbb|aaaa..bbbb|aaaa..bbbb|..........|..cccc....|..cccc....|..cccc....|..cccc....|..........'],
snow:['a#bfefff','....aa....|.a..aa..a.|..a.aa.a..|...aaaa...|aaaaaaaaaa|aaaaaaaaaa|...aaaa...|..a.aa.a..|.a..aa..a.|....aa....'],
moon:['a#e8e0b0','...aaaa...|..aaa.....|.aaaa.....|.aaaa.....|.aaaa.....|.aaaa.....|.aaaaa....|..aaaaa...|...aaaaaa.|....aaaa..'],
move:['a#9f8cff,b#ffffff','....aa....|...aaaa...|....aa....|.a..bb..a.|aaa.bb.aaa|aaa.bb.aaa|.a..bb..a.|....aa....|...aaaa...|....aa....'],
search:['a#c9b8ff,c#ffffff','..aaaa....|.a....a...|a..cc..a..|a..cc..a..|a......a..|.a....a...|..aaaa....|.....aa...|......aa..|.......aa.'],
lever:['a#ffa020,b#4a3a70,c#2a2250','.......aa.|......aa..|.....aa...|....aa....|...aa.....|bbbbbbbbbb|bccccccccb|bccccccccb|bbbbbbbbbb|..........'],
door:['a#8a6a3a,b#c8a060,c#4a3010,d#ffd23f','..aaaaaa..|.abbbbbba.|.abccccba.|.abccccba.|.abccccba.|.abcccdba.|.abccccba.|.abccccba.|.abccccba.|aaaaaaaaaa'],
spikes:['a#ff4a6a,b#4a2a3a','..a...a...|..a...a...|.aaa.aaa..|.aaa.aaa..|aaaaaaaaaa|aaaaaaaaaa|bbbbbbbbbb|bbbbbbbbbb|..........|..........'],
gem:['a#2ad0d0,b#55ffff,c#ffffff','..aaaaaa..|.abcbbcba.|abbcbbcbba|aaaaaaaaaa|.abbcbbba.|..abcbba..|...abba...|....aa....|..........|..........'],
hand:['a#f0dcc4','...aa.....|...aa.....|...aa.aa..|.a.aa.aa..|.a.aaaaaa.|.aaaaaaaaa|.aaaaaaaaa|..aaaaaaa.|...aaaaa..|...aaaaa..'],
target:['a#ffd23f,b#ff5c7a','...aaaa...|..a....a..|.a.aaaa.a.|a.a....a.a|a.a.bb.a.a|a.a.bb.a.a|a.a....a.a|.a.aaaa.a.|..a....a..|...aaaa...']};
const ICON={};
(function(){for(const k in ICONS){const pal={};ICONS[k][0].split(',').forEach(s=>pal[s[0]]=s.slice(1));const c=document.createElement('canvas');c.width=c.height=10;const g=c.getContext('2d');
ICONS[k][1].split('|').forEach((row,y)=>[...row].forEach((ch,x)=>{if(pal[ch]){g.fillStyle=pal[ch];g.fillRect(x,y,1,1)}}));ICON[k]=c.toDataURL()}})();
function ico(n,s){s=s||18;return '<i class="ic" style="background-image:url('+ICON[n]+');width:'+s+'px;height:'+s+'px"></i>'}
function setIc(e,n){if(e._i!==n){e._i=n;e.style.backgroundImage='url('+ICON[n]+')'}}
function applyIcons(){document.querySelectorAll('[data-ic]').forEach(e=>setIc(e,e.dataset.ic))}
