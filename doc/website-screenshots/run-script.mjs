// Executes a Kotlin script (ad hoc, like the script page) on a running ProjectForge instance.
// Usage: node run-script.mjs <script.kts>
// Env: PF_URL (default http://localhost:8085), PF_USER/PF_PASSWORD (default laura), PW_PACKAGE (package.json
// of a checkout with installed playwright, default ../../projectforge-next/package.json).
import { createRequire } from "module";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const here = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(process.env.PW_PACKAGE || path.join(here, "../../projectforge-next/package.json"));
const { request } = require("playwright");
const baseURL = process.env.PF_URL || "http://localhost:8085";
const username = process.env.PF_USER || "laura";
const password = process.env.PF_PASSWORD || "Screenshots-2026!";

const ctx = await request.newContext({ baseURL });
let res = await ctx.post("/rsPublic/nextLogin", { data: { username, password } });
if (!res.ok() || (await res.json())?.status !== "SUCCESS") {
  console.error(`Login as ${username} failed (HTTP ${res.status()}).`);
  process.exit(1);
}
const { csrfToken } = await (await ctx.get("/rs/userStatus")).json();
res = await ctx.post("/rs/scriptExecute/execute", {
  headers: { "X-PF-CSRF-Token": csrfToken },
  data: { type: "KOTLIN", script: fs.readFileSync(process.argv[2], "utf8") },
  timeout: 900_000,
});
const result = await res.json();
console.log(result.result);
if (result.hasErrors) {
  console.log((await (await ctx.get("/rs/scriptExecute/refresh")).text()).slice(0, 4000));
  process.exit(1);
}
