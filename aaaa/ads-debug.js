// Ad diagnostics. Open the game with ?adsdebug=1 to see whether the Adsterra script is reachable and whether it rendered an ad.
(function(){if(!/[?&]adsdebug=1/.test(location.search))return;
var s=document.querySelector('#adsterra-slot script[src]');var box=document.createElement('div');box.id='ads-debug';document.body.appendChild(box);
var log=['host: '+location.host,'script: '+(s?s.src:'NOT FOUND')];function show(){box.textContent=log.join('\n')}show();
if(s){fetch(s.src,{mode:'no-cors'}).then(function(){log.push('network: script URL reachable');show()}).catch(function(){log.push('network: BLOCKED or unreachable (ad blocker / DNS / network / wrong URL)');show()})}
setTimeout(function(){var c=document.querySelector('#adsterra-slot [id^=container-]');log.push('container children after 8s: '+(c?c.children.length:'no container')+(c&&c.children.length?' (ad rendered)':' (no ad rendered)'));show()},8000)})();
