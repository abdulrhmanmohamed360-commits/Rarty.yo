// Leaderboard integration layer. NO server is connected in this build, so no global rankings are shown or invented.
// To go live: deploy a backend, set API.base to its URL, and implement the contract in README.md.
// The server MUST validate every run (never trust clientScore): recompute score from timeMs/attempts/secret,
// reject impossible times, rate-limit per player id, and ideally verify an input log / signed run token.
const API={base:'',
configured(){return !!this.base},
async top(){const r=await fetch(this.base+'/v1/leaderboard?limit=100&player='+encodeURIComponent(save.pid||''),{headers:{Accept:'application/json'}});if(!r.ok)throw new Error('http '+r.status);return r.json()},
async submit(run){if(!this.configured())return false;try{const r=await fetch(this.base+'/v1/runs',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(run)});return r.ok}catch(e){return false}}};
