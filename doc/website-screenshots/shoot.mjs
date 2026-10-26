// Takes the website screenshots of the scenes in scenes.mjs as user laura.
// Usage: node shoot.mjs [outDir] [scene,scene,...]   (default outDir: /tmp/pf-screenshots)
// Env: PF_URL, PF_USER/PF_PASSWORD, PW_PACKAGE as in run-script.mjs.
// The pictures are taken at 1440 px width with device scale 2; resize them for the website, e.g.
// sips -Z 1920 <file> --out site/uploads/screenshots/<file>
import { createRequire } from "module";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const here = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(process.env.PW_PACKAGE || path.join(here, "../../projectforge-next/package.json"));
const { chromium } = require("playwright");
const base = (process.env.PF_URL || "http://localhost:8085") + "/next";
const outDir = process.argv[2] || "/tmp/pf-screenshots";
const only = process.argv[3]?.split(",");
const scenes = (await import(path.join(here, "scenes.mjs"))).default.filter((s) => !only || only.includes(s.name));
fs.mkdirSync(outDir, { recursive: true });

const browser = await chromium.launch();
for (const dark of [false, true]) {
  const list = scenes.filter((s) => !!s.dark === dark);
  if (!list.length) continue;
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 860 }, deviceScaleFactor: 2, locale: "en-US", colorScheme: dark ? "dark" : "light" });
  const page = await ctx.newPage();
  await page.goto(base + "/login");
  await page.waitForTimeout(1500);
  await page.fill("#username", process.env.PF_USER || "laura");
  await page.fill("#password", process.env.PF_PASSWORD || "Screenshots-2026!");
  await page.locator('button[type="submit"]').click();
  await page.waitForURL((u) => !u.toString().includes("/login"), { timeout: 20_000 });
  for (const s of list) {
    try {
      await page.setViewportSize({ width: 1440, height: s.height || 860 });
      await page.goto(base + s.path);
      await page.waitForLoadState("networkidle").catch(() => {});
      await page.waitForTimeout(s.wait || 2000);
      if (s.act) await s.act(page);
      await page.waitForTimeout(800);
      await page.screenshot({ path: path.join(outDir, `${s.name}.png`) });
      console.log("ok", s.name);
    } catch (e) {
      console.log("FAILED", s.name, e.message.split("\n")[0]);
    }
  }
  await ctx.close();
}
await browser.close();
