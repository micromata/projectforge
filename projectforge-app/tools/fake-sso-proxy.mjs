#!/usr/bin/env node
/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

// A fake SSO proxy in forward auth mode (the way of Authentik's outpost in nginx), for testing the app behind a
// proxy without one: the app's SSO handling (proxy cookies of the login, refusals, renewal in the system browser).
//
//   node tools/fake-sso-proxy.mjs [--upstream http://localhost:8081] [--port 8091] [--login-port 8092]
//                                 [--ttl 120] [--sso-ttl 3600] [--interactive] [--guard-public]
//
// - http://localhost:<port> forwards to the upstream ProjectForge while the browser has a valid proxy cookie
//   (authentik_proxy_test, valid for --ttl seconds).
// - Without one, a page (Accept: text/html) is redirected to /outpost.goauthentik.io/start, which redirects to the
//   login of the SSO on another host (http://127.0.0.1:<login-port>, as sso.example.org would be), which redirects
//   back to /outpost.goauthentik.io/callback, which sets the proxy cookie and redirects to the page asked for.
//   Any other request (the api) is refused with a 401 text/html, not the json of ProjectForge.
// - The SSO keeps its own session (--sso-ttl): a second login goes through without a form, as ASWebAuthenticationSession
//   shares the cookies with Safari. --interactive: the SSO always shows its form (a button), e.g. for the app's
//   behaviour when the user cancels.
//
// The server needs `projectforge.app.proxyCookies=authentik_proxy_*`, otherwise it hands no proxy cookies to the app.

import { randomBytes } from 'node:crypto';
import http from 'node:http';

const args = process.argv.slice(2);
const option = (name, fallback) => {
  const index = args.indexOf(`--${name}`);
  return index >= 0 ? args[index + 1] : fallback;
};
const UPSTREAM = new URL(option('upstream', 'http://localhost:8081'));
const PORT = Number(option('port', '8091'));
const LOGIN_PORT = Number(option('login-port', '8092'));
const TTL = Number(option('ttl', '120')) * 1000;
const SSO_TTL = Number(option('sso-ttl', '3600')) * 1000;
const INTERACTIVE = args.includes('--interactive');
const GUARD_PUBLIC = args.includes('--guard-public');

const PROXY_ORIGIN = `http://localhost:${PORT}`;
const SSO_ORIGIN = `http://127.0.0.1:${LOGIN_PORT}`;
const PROXY_COOKIE = 'authentik_proxy_test';
const SSO_COOKIE = 'sso_session';

const log = (...parts) => console.log(new Date().toISOString().substring(11, 19), ...parts);

function cookies(req) {
  return Object.fromEntries(
    (req.headers.cookie ?? '')
      .split(';')
      .map((it) => it.trim())
      .filter(Boolean)
      .map((it) => [it.substring(0, it.indexOf('=')), it.substring(it.indexOf('=') + 1)]),
  );
}

/** `<expires>.<random>`, valid until expires. */
const newSession = (ttl) => `${Date.now() + ttl}.${randomBytes(12).toString('hex')}`;
const valid = (value) => value != null && Number(value.split('.')[0]) > Date.now();

function redirect(res, location, extraHeaders = {}) {
  res.writeHead(302, { Location: location, 'Cache-Control': 'no-store', ...extraHeaders });
  res.end();
}

/** Only back to the proxy's own origin. */
function safeReturn(rd) {
  try {
    const url = new URL(rd ?? '/', PROXY_ORIGIN);
    return url.origin === PROXY_ORIGIN ? url.href : `${PROXY_ORIGIN}/`;
  } catch {
    return `${PROXY_ORIGIN}/`;
  }
}

function proxyHandler(req, res) {
  const url = new URL(req.url, PROXY_ORIGIN);
  if (url.pathname === '/outpost.goauthentik.io/start') {
    const rd = safeReturn(url.searchParams.get('rd'));
    log('proxy', 'start → SSO login', rd);
    redirect(res, `${SSO_ORIGIN}/authorize?rd=${encodeURIComponent(rd)}`);
    return;
  }
  if (url.pathname === '/outpost.goauthentik.io/callback') {
    const rd = safeReturn(url.searchParams.get('rd'));
    log('proxy', 'callback: new proxy session →', rd);
    redirect(res, rd, {
      'Set-Cookie': `${PROXY_COOKIE}=${newSession(TTL)}; Path=/; HttpOnly; SameSite=Lax; Max-Age=${TTL / 1000}`,
    });
    return;
  }
  // The public api passes (system status, the token exchange of the app login, which has no proxy cookies yet), as
  // in an installation behind a proxy; --guard-public guards it too.
  const open = url.pathname.startsWith('/rsPublic/') && !GUARD_PUBLIC;
  if (!open && !valid(cookies(req)[PROXY_COOKIE])) {
    const page = (req.headers.accept ?? '').includes('text/html');
    if (page) {
      log('proxy', 'refused (page) →', url.pathname);
      redirect(res, `${PROXY_ORIGIN}/outpost.goauthentik.io/start?rd=${encodeURIComponent(url.href)}`);
    } else {
      log('proxy', 'refused (401) →', req.method, url.pathname);
      res.writeHead(401, { 'Content-Type': 'text/html' });
      res.end('<html><body>Unauthorized by the SSO proxy</body></html>');
    }
    return;
  }
  const upstream = http.request(
    {
      host: UPSTREAM.hostname,
      port: UPSTREAM.port,
      method: req.method,
      path: req.url,
      headers: { ...req.headers, 'X-Forwarded-Host': req.headers.host, 'X-Forwarded-Proto': 'http' },
    },
    (answer) => {
      res.writeHead(answer.statusCode ?? 502, answer.headers);
      answer.pipe(res);
    },
  );
  upstream.on('error', (e) => {
    log('proxy', 'upstream error', e.message);
    if (!res.headersSent) res.writeHead(502, { 'Content-Type': 'text/plain' });
    res.end('Bad gateway');
  });
  req.pipe(upstream);
}

function ssoHandler(req, res) {
  const url = new URL(req.url, SSO_ORIGIN);
  const rd = safeReturn(url.searchParams.get('rd'));
  const callback = `${PROXY_ORIGIN}/outpost.goauthentik.io/callback?rd=${encodeURIComponent(rd)}`;
  if (url.pathname === '/authorize') {
    if (!INTERACTIVE && valid(cookies(req)[SSO_COOKIE])) {
      log('sso', 'session of the SSO still valid → callback');
      redirect(res, callback);
      return;
    }
    log('sso', 'login form');
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(`<!doctype html><html><head><meta name="viewport" content="width=device-width"><title>Fake SSO</title></head>
<body style="font-family:sans-serif;padding:2em">
<h2>Fake SSO login</h2><p>On ${SSO_ORIGIN}, for ${PROXY_ORIGIN}.</p>
<form method="post" action="/login?rd=${encodeURIComponent(rd)}"><button style="font-size:1.4em" type="submit">Log in at the SSO</button></form>
</body></html>`);
    return;
  }
  if (url.pathname === '/login' && req.method === 'POST') {
    log('sso', 'logged in → callback');
    redirect(res, callback, {
      'Set-Cookie': `${SSO_COOKIE}=${newSession(SSO_TTL)}; Path=/; HttpOnly; SameSite=Lax; Max-Age=${SSO_TTL / 1000}`,
    });
    return;
  }
  res.writeHead(404);
  res.end();
}

/** On both loopback addresses (localhost may resolve to ::1 or 127.0.0.1 first), never on the network. */
function listenLoopback(handler, port, hosts) {
  for (const host of hosts) http.createServer(handler).listen(port, host).on('error', (e) => log(host, e.message));
}

listenLoopback(proxyHandler, PORT, ['127.0.0.1', '::1']);
listenLoopback(ssoHandler, LOGIN_PORT, ['127.0.0.1']);
log(`proxy ${PROXY_ORIGIN} → ${UPSTREAM.origin}, proxy session ${TTL / 1000} s, cookie ${PROXY_COOKIE}`);
log(`SSO login ${SSO_ORIGIN}, its session ${SSO_TTL / 1000} s${INTERACTIVE ? ', always interactive' : ''}`);
