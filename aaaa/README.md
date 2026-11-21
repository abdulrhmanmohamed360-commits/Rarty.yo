# CHAOS ROOM v0.5.1
2D top-down puzzle escape game. Vanilla HTML5 Canvas/JS, no dependencies, no combat.

## Run / Deploy
- Local: open `index.html` (or run `python3 -m http.server` and open it on your phone).
- Netlify: drag the unzipped `chaos-room` folder (the one containing `index.html`) onto https://app.netlify.com/drop. The ZIP's files sit inside a `chaos-room/` folder, so unzip first.
- Offline / no hosting: `chaos-room-single.html` is the whole game in one file.

## Controls
PC: WASD / arrows, E or Space to interact, P / Esc to pause. Mobile: joystick + ACT (Settings → Left-handed swaps sides).

## Changelog (0.5.1)
- All emoji removed. Every icon (HUD, rules, tutorial, how-to, achievements, room cards) is now hand-made pixel art drawn from text bitmaps in `icons.js`.

## Changelog (0.5)
- 2 new rooms (20 total): Room 19 "Gem Vault" (secret gem behind a cracked wall, bridge + ghoul chamber) and Room 20 "Final Descent" (121 tiles, portal, bridges, fast ghouls, levers; hardest room).
- Controls: joystick fixed on the LEFT and the ACT button on the RIGHT (also in Arabic mode; Settings > Left-handed swaps them). The floating joystick follows your thumb on the left side.
- Clearer top bar: icons for each rule, a bigger rule panel that flashes red during warnings with a plain explanation and countdown, key/coin/timer icons, larger hints with better contrast, clearer Arabic/English wording.
- Graphics: beveled stone walls with depth, shaded floors, soft wall shadows, theme decorations (bones, crystals, mushrooms, urns), baked torch light pools, rising embers, footstep dust, new outlined hero with a swinging lantern, lantern glow, per-theme color grading, flash when a rule starts.

## Changelog (0.4)
- 10 new LONG rooms (18 total): 46 to 106 tiles wide with a scrolling camera, 3 to 7 chambers each, much harder (rooms 9-18 rated Hard to Nightmare). Longer chaos events and shorter gaps in later rooms.
- Monsters (no combat, only evasion): sleeping ghouls that wake, chase and can be outrun; fast ghouls; vertical sweepers.
- New fun mechanics: conveyor belts, a portal pair, collectible coins (+40 score each), animated pits and bridges.
- Fewer checkpoints: removed from rooms 3, 4 and 5; each new room has exactly one.
- Easier controls: floating joystick (appears under your thumb on the movement side, bigger), bigger ACT button, auto-interact for switches (toggle in Settings), smaller hitbox and corner sliding, slightly faster hero.
- Graphics: six room color themes (crypt, teal, crimson, moss, ice, gold), drifting dust, ghoul/belt/portal/coin animations.
- Room 7 lever switches and secret gem are unchanged; all earlier features kept.

## Changelog (0.3)
- Full Arabic + English: language buttons in Settings, instant switch, saved choice, English fallback, RTL layout, Arabic-safe fonts/spacing. All UI text, room names/hints, tutorial, messages, achievements and leaderboard are translated.
- Translations live only in `i18n.js`. To add a language: copy the `en` block, translate, add a `data-a="lang" data-l="xx"` button.
- Graphics: pre-rendered stone floors/walls with depth, wall shadows, moss/cracks/runes, torches with glow, new animated hero, animated gates/plates/flags/door, pits and bridges, chaos effects for gravity/freeze/reverse, room title card, cached layers for low-end phones.
- Exactly 3 new rooms (8 total): 06 The Timing (spike bands, blinking bridges over a pit, timed plate, switch), 07 The Shift (levers flip the purple walls, secret gem), 08 The Chaos (fixed chaos order with a next-shift preview, checkpoint, two-stage gates). The 5 original rooms are unchanged.
- New tiles: V pit, B blinking bridge, X wall lever. Chaos never starts while you stand next to pits/bridges/spikes (reverse, gravity, freeze).
- Room select: cards with number, name, difficulty, locked/cleared state, best time; locked rooms cannot be started.
- New soft SFX: wall shift, mechanism, bridge tick. Music and volume controls unchanged.
- Save compatible with 0.2. Note: 0.2 rooms 6-7 were replaced, so their old scores/times are cleared and unlock is capped at room 6.

## Changelog (0.2)
Animated menu, tutorial, calm music, volume settings, scoring, achievements, leaderboard layer, secrets.

## Leaderboard backend contract (for later)
Set `API.base` in `api.js`.
- `GET /v1/leaderboard?limit=100&player=<pid>` → `{players:[{name,score,rooms,bestTime}], me:{rank}}`
- `POST /v1/runs` body `{name,pid,room,timeMs,attempts,secret,clientScore,v}`.
The server must recompute the score itself, reject impossible times, rate-limit, and ideally verify a signed run token or input log. Never trust `clientScore`.

## Not done yet
Key remapping, true moving platforms (blinking bridges are used instead), a minimap for the long rooms, real leaderboard server, Arabic text inside the game canvas (all Arabic text is in the HTML UI).

## Ads integration (Google H5 Games Ads)
- `ads-config.js` is intentionally disabled by default. The game remains fully playable without an ad account or ad availability.
- Google H5 Games Ads requires an approved AdSense account and separate approval for H5 Games Ads access. Approval is not guaranteed.
- After approval, replace the example publisher ID in `ads-config.js` with your real `ca-pub-...` publisher ID and set `enabled: true`.
- The integration counts completed rooms in the current page visit. It offers an interstitial only after room 3 and room 6, when the player presses **Next**. This means at most two ad opportunities per visit. There are no ads before entering or during active gameplay. A fresh page load starts a new cycle. If no ad is available, the game continues normally.
- Do not add ordinary AdSense content units close to game controls or gameplay. Follow Google's current game-ad placement and publisher policies.

## Ads (Adsterra Native Banner) - v0.5.3
- Ad break after every 3 completed rooms, max 3 per visit (set in ads-config.js). Shown only if an ad actually rendered; Continue unlocks after 5s.
- The banner shows only on the main menu. It is kept laid out (hidden, not display:none) outside the menu so the ad script can render.
- Ads only load on the domain approved in your Adsterra account (not on file://, localhost, or the claude.ai preview link).
- Diagnose: open your site with `?adsdebug=1` at the end of the address to see if the ad script is reachable and if it rendered.
