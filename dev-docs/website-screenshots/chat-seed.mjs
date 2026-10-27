// Creates the sample chat of the website screenshot: the room "Fleet Tracking Platform" with messages of several
// sample users (mentions, code block, link, image, reactions, a thread), a second room and a direct message with
// unread messages for laura. Skips everything if the room exists already.
// Usage: node chat-seed.mjs
// Env: PF_URL, PF_PASSWORD (password of all sample users), PW_PACKAGE as in run-script.mjs.
import { createRequire } from "module";
import path from "path";
import { fileURLToPath } from "url";

const here = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(process.env.PW_PACKAGE || path.join(here, "../../projectforge-next/package.json"));
const { request, chromium } = require("playwright");
const baseURL = process.env.PF_URL || "http://localhost:8085";
const password = process.env.PF_PASSWORD || "Screenshots-2026!";
const ROOM = "Fleet Tracking Platform";

const sessions = {};
async function session(username) {
  if (sessions[username]) return sessions[username];
  const ctx = await request.newContext({ baseURL });
  const res = await ctx.post("/rsPublic/nextLogin", { data: { username, password } });
  if (!res.ok() || (await res.json())?.status !== "SUCCESS") throw new Error(`Login as ${username} failed (HTTP ${res.status()}).`);
  const status = await (await ctx.get("/rs/userStatus")).json();
  const s = { ctx, id: status.userData.userId, name: status.userData.fullname, headers: { "X-PF-CSRF-Token": status.csrfToken } };
  sessions[username] = s;
  return s;
}

async function check(res, what) {
  if (!res.ok()) throw new Error(`${what}: HTTP ${res.status()} ${(await res.text()).slice(0, 300)}`);
  return res.status() === 200 && (res.headers()["content-type"] || "").includes("json") ? res.json() : null;
}

const mention = (s) => `<span data-type="mention" data-id="${s.id}">@${s.name}</span>`;

async function send(username, conversationId, html, { threadRootId, file } = {}) {
  const s = await session(username);
  const url = `/rs/chat/conversation/${conversationId}/messages`;
  const res = file
    ? await s.ctx.post(url, {
        headers: s.headers,
        multipart: {
          message: { name: "message.json", mimeType: "application/json", buffer: Buffer.from(JSON.stringify({ html, threadRootId })) },
          files: file,
        },
      })
    : await s.ctx.post(url, { headers: s.headers, data: { html, threadRootId } });
  return check(res, `send as ${username}`);
}

async function react(username, messageId, emoji) {
  const s = await session(username);
  await check(await s.ctx.post(`/rs/chat/message/${messageId}/reaction`, { headers: s.headers, data: { emoji } }), `react as ${username}`);
}

async function read(username, conversationId, messageId) {
  const s = await session(username);
  await check(await s.ctx.post(`/rs/chat/conversation/${conversationId}/read`, { headers: s.headers, data: { messageId } }), `read as ${username}`);
}

async function room(name, description, usernames, groups = []) {
  const laura = await session("laura");
  const users = [];
  for (const u of usernames) users.push({ id: (await session(u)).id, displayName: "" });
  return check(
    await laura.ctx.post("/rs/chatRoom/save", { headers: laura.headers, data: { name, description, users, groups } }),
    `room ${name}`,
  );
}

/** A mockup of the new map view, rendered by the browser: the image of the chat message. */
async function mapMockup() {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 640, height: 400 }, deviceScaleFactor: 2 });
  const trucks = [[120, 90, "#2563eb"], [300, 210, "#16a34a"], [470, 120, "#f59e0b"], [520, 300, "#2563eb"], [200, 300, "#dc2626"]];
  await page.setContent(`<body style="margin:0;font-family:system-ui,sans-serif">
    <div style="position:relative;width:640px;height:400px;background:#e8eef3;overflow:hidden">
      <svg width="640" height="400" style="position:absolute">
        <path d="M0 260 C 150 200, 250 330, 640 240" stroke="#fff" stroke-width="18" fill="none"/>
        <path d="M180 0 C 220 150, 120 260, 260 400" stroke="#fff" stroke-width="14" fill="none"/>
        <path d="M420 0 L 470 400" stroke="#fff" stroke-width="10" fill="none"/>
        <rect x="40" y="40" width="90" height="60" fill="#d4e4d4" rx="6"/><rect x="520" y="40" width="80" height="50" fill="#d4e4d4" rx="6"/>
        <path d="M120 90 C 200 140, 250 180, 300 210" stroke="#2563eb" stroke-width="4" stroke-dasharray="8 6" fill="none"/>
      </svg>
      ${trucks.map(([x, y, c]) => `<div style="position:absolute;left:${x - 13}px;top:${y - 13}px;width:26px;height:26px;border-radius:50%;background:${c};border:3px solid #fff;box-shadow:0 2px 6px #0004"></div>`).join("")}
      <div style="position:absolute;left:16px;top:16px;background:#fff;border-radius:10px;padding:10px 14px;box-shadow:0 2px 10px #0002;font-size:14px">
        <b>Fleet overview</b><br><span style="color:#16a34a">● 42 on the road</span> · <span style="color:#f59e0b">● 7 loading</span> · <span style="color:#dc2626">● 1 delayed</span>
      </div>
    </div></body>`);
  const buffer = await page.screenshot();
  await browser.close();
  return { name: "Fleet-Map_v2.png", mimeType: "image/png", buffer };
}

const laura = await session("laura");
const existing = await check(await laura.ctx.get("/rs/chatRoom/list"), "room list");
if (existing.some((r) => r.name === ROOM)) {
  console.log(`Room "${ROOM}" exists already, nothing done.`);
  process.exit(0);
}

const fleet = await room(ROOM, "Go-live of release 2.4 for Bluefin Logistics", ["joe", "alex", "ann", "max", "chris"]);
const c = fleet.conversationId;
const [alex, ann, max] = await Promise.all(["alex", "ann", "max"].map(session));

const m1 = await send("joe", c, "<p>Morning! Bluefin moved the go-live of release <strong>2.4</strong> to Friday. Can we make it?</p>");
const m2 = await send("alex", c, `<p>Backend is ready, only the GPS import still fails for one tracker model:</p><pre><code>./gradlew :tracking-import:test --tests "*GpsParserTest"
GpsParserTest &gt; parsesTeltonikaFrames FAILED</code></pre>`);
const m3 = await send("ann", c, "<p>The new map view is done 🎉 Trucks are coloured by status now.</p>", { file: await mapMockup() });
const m4 = await send("laura", c, `<p>${mention(ann)} looks great! ${mention(alex)} let's look at the import together at 2 pm.</p>`);
await send("max", c, `<p>I'm writing the <span style="color: #16a34a">release notes</span>: <a href="https://example.org/fleet/release-2.4">example.org/fleet/release-2.4</a></p>`);

await send("max", c, "<p>Found it – the timestamps of that model come in UTC, not in local time. The fix is in review.</p>", { threadRootId: m2.id });
await send("alex", c, "<p>Thanks, merged ✔️ All tests green.</p>", { threadRootId: m2.id });
await send("joe", c, "<p>Perfect, then Friday it is 👌</p>", { threadRootId: m2.id });

for (const [u, e] of [["alex", "👍"], ["max", "👍"], ["laura", "👍"]]) await react(u, m1.id, e);
for (const [u, e] of [["laura", "🎉"], ["joe", "🎉"], ["max", "🎉"], ["joe", "❤️"]]) await react(u, m3.id, e);
await react("ann", m4.id, "🙏");
await read("laura", c, m4.id);

// Unread for laura in the sidebar: a second room and a direct message.
const leads = await room("Delivery leads", "Staffing and planning across projects", ["joe", "julia"]);
await send("joe", leads.conversationId, "<p>Can we move Chris to the Claims Portal for two weeks in November?</p>");
await send("julia", leads.conversationId, "<p>Fine with me, the archive project can wait until December.</p>");

const direct = await check(await max.ctx.post("/rs/chatDirect/open", { headers: max.headers, data: { userId: laura.id } }), "direct");
await send("max", direct.conversationId, "<p>Hi Laura, do you have five minutes for the sprint planning later?</p>");

console.log(`Room "${ROOM}": /next/chat/${c}`);
