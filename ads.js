// Google H5 Games Ads integration for CHAOS ROOM.
// Ad opportunities happen only after every 3 completed rooms, at most twice
// per page visit. No ad is requested before entering or during active play.
(function () {
  'use strict';
  const cfg = window.CHAOS_AD_CONFIG || {};
  let roomsSinceAd = 0;
  let adOpportunitiesUsed = 0;
  let movingToNextRoom = false;

  function roomCompleted() {
    // Count completed rooms, not retries, menu visits, or page loads.
    if (adOpportunitiesUsed < 2 && roomsSinceAd < 3) roomsSinceAd += 1;
  }

  function showNextRoom(continueGame) {
    if (movingToNextRoom) return;
    movingToNextRoom = true;

    const proceed = function () {
      if (!movingToNextRoom) return;
      movingToNextRoom = false;
      if (typeof continueGame === 'function') continueGame();
    };

    // Only the 3rd and 6th completed rooms create an ad opportunity.
    if (roomsSinceAd < 3 || adOpportunitiesUsed >= 2) {
      proceed();
      return;
    }

    // Consume this visit's opportunity even if the ad network has no fill.
    roomsSinceAd = 0;
    adOpportunitiesUsed += 1;

    // Safe fallback while ads are disabled, unapproved, or unavailable.
    if (!cfg.enabled || !/^ca-pub-\d+$/.test(cfg.publisherId || '') ||
        typeof window.adBreak !== 'function' || !window.__CHAOS_ADS_READY) {
      proceed();
      return;
    }

    let completed = false;
    const finish = function () {
      if (completed) return;
      completed = true;
      proceed();
    };

    try {
      window.adBreak({
        type: 'next',
        name: adOpportunitiesUsed === 1 ? 'after_room_3' : 'after_room_6',
        adBreakDone: finish
      });
    } catch (err) {
      console.warn('CHAOS ROOM ad placement unavailable:', err);
      finish();
    }
  }

  // These counters live only in memory, so a fresh page visit starts a new cycle.
  window.CHAOS_ADS = {
    roomCompleted: roomCompleted,
    showNextRoom: showNextRoom
  };

  if (!cfg.enabled || !/^ca-pub-\d+$/.test(cfg.publisherId || '')) return;

  // Google H5 Games Ads access requires separate approval.
  window.adsbygoogle = window.adsbygoogle || [];
  window.adBreak = window.adConfig = function (options) {
    window.adsbygoogle.push(options);
  };

  const script = document.createElement('script');
  script.async = true;
  script.crossOrigin = 'anonymous';
  script.setAttribute('data-ad-frequency-hint', cfg.frequencyHint || '120s');
  script.src = 'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=' +
    encodeURIComponent(cfg.publisherId);
  script.onload = function () {
    window.__CHAOS_ADS_READY = true;
    try { window.adConfig({ preloadAdBreaks: 'on', sound: 'off' }); }
    catch (err) { console.warn('CHAOS ROOM ad config failed:', err); }
  };
  script.onerror = function () {
    window.__CHAOS_ADS_READY = false;
    console.warn('CHAOS ROOM ads script could not load. Game will continue without ads.');
  };
  document.head.appendChild(script);
})();
