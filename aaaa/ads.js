// Ad breaks: after every 3rd completed room (max 3 per visit) the player sees a short "Advertisement" screen
// that shows the Adsterra Native Banner, then taps Continue. If no ad rendered (blocked / no fill), the break
// is skipped silently and tried again after the next room, so players are never stuck.
(function () {
  'use strict';
  const cfg = window.CHAOS_AD_CONFIG || {};
  if (/[?&]adsdebug=1/.test(location.search)) cfg.breakEvery = 1; // test mode: ad break after every room
  const every = cfg.breakEvery || 3, maxBreaks = cfg.maxBreaks || 3, wait = cfg.waitSeconds || 5;
  let roomsSinceAd = 0, used = 0, busy = false;

  function adRendered() {
    const c = document.querySelector('#adsterra-slot [id^=container-]');
    return !!(c && c.children && c.children.length);
  }
  function roomCompleted() { roomsSinceAd += 1; }

  function showNextRoom(cont) {
    const go = function () { if (typeof cont === 'function') cont(); };
    if (busy) return;
    if (roomsSinceAd < every || used >= maxBreaks || !adRendered()) { go(); return; }
    busy = true; used += 1; roomsSinceAd = 0;
    const btn = document.getElementById('adgo'), msg = document.getElementById('adwait');
    let n = wait, timer = null;
    document.body.classList.add('adbreak');
    btn.disabled = true; btn.textContent = tr('adcont'); msg.textContent = tr('adwait', { n: n });
    timer = setInterval(function () {
      n -= 1;
      if (n > 0) { msg.textContent = tr('adwait', { n: n }); return; }
      clearInterval(timer); msg.textContent = ''; btn.disabled = false;
    }, 1000);
    btn.onclick = function () {
      if (btn.disabled) return;
      clearInterval(timer);
      document.body.classList.remove('adbreak');
      busy = false; go();
    };
  }
  window.CHAOS_ADS = { roomCompleted: roomCompleted, showNextRoom: showNextRoom };
})();
