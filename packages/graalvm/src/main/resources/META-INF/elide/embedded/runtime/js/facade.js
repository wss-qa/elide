(function(){var d=this||self,a=globalThis;function i(o,r){var e=o.split("."),t=d;e[0]in t||typeof t.b=="undefined"||t.b("var "+e[0]);for(var s;e.length&&(s=e.shift());)e.length||r===void 0?t[s]&&t[s]!==Object.prototype[s]?t=t[s]:t=t[s]={}:t[s]=r;a[o]=r}var h=class extends Error{constructor(o){super(o);this.a=!0,this.message=o||"`ValueError` was thrown"}static[Symbol.hasInstance](o){return o&&o.a===!0||!1}};i("ValueError",h);class g{typeError(r,e){{var t=TypeError;let s;if(typeof r=="string"?s=r:s=r.message,r=new t(s),e?.c==0)e=r;else throw r}return e}valueError(r,e){let t;if(typeof r=="string"?t=r:t=r.message,r=new h(t),e?.c===!1)return r;throw r}}i("__errBridge",new g),i("btoa",function(o){return a.Base64.encode(o)}),i("atob",function(o){return a.Base64.decode(o)});function n(){return Error("Method not supported")}class b{trace(...r){l("debug",r)}log(...r){l("debug",r)}debug(...r){l("debug",r)}info(...r){l("info",r)}warn(...r){l("warn",r)}error(...r){l("error",r)}assert(){throw Error("Method not implemented.")}clear(){}count(){throw n()}countReset(){throw n()}dir(){throw n()}dirxml(){throw n()}group(){throw n()}groupCollapsed(){throw n()}groupEnd(){throw n()}table(){throw n()}time(){throw n()}timeEnd(){throw n()}timeLog(){throw n()}}function l(o,r){let e=a.Console;switch(o){case"trace":e.log(r);break;case"debug":e.log(r);break;case"info":e.info(r);break;case"warn":e.warn(r);break;case"error":e.error(r)}}i("console",new class extends b{});let u={pid:-1,cwd:()=>"",env:{},NODE_DEBUG:!1,NODE_ENV:"production",noDeprecation:!1,browser:!1,version:"v18.9.0"};globalThis.process=u,globalThis.window=void 0,globalThis.gc=null;let c={};globalThis.global=c,globalThis.self=c,globalThis.Elide={process:u,self:globalThis,context:{build:!1,runtime:!0},App:c}}).call({});
// Elide JS Runtime. Copyright (c) 2022, Sam Gammon and the Elide Project Authors. All rights reserved.
// Components of this software are licensed separately. See https://github.com/elide-dev/v3 for more.
// Built: Jan 14th, 2023, 18:06:00 PST.
