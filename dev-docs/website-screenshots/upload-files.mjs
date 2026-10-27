// Uploads the sample files of ./files into the data transfer area "Alpine Insurance – Claims Portal".
// The files only need a name and a size; create them with ./create-files.sh. Env as in run-script.mjs.
import { createRequire } from "module";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const here = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(process.env.PW_PACKAGE || path.join(here, "../../projectforge-next/package.json"));
const { chromium } = require("playwright");
const base = (process.env.PF_URL || "http://localhost:8085") + "/next";
const dir = path.join(here, "files");

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1440, height: 860 }, locale: "en-US" });
await page.goto(base + "/login");
await page.waitForTimeout(1500);
await page.fill("#username", process.env.PF_USER || "laura");
await page.fill("#password", process.env.PF_PASSWORD || "Screenshots-2026!");
await page.locator('button[type="submit"]').click();
await page.waitForURL((u) => !u.toString().includes("/login"), { timeout: 20_000 });
await page.goto(base + "/datatransfer");
await page.waitForTimeout(2500);
await page.getByText("Alpine Insurance – Claims Portal").first().click();
await page.waitForTimeout(2500);
for (const f of fs.readdirSync(dir).filter((f) => !f.startsWith("."))) {
  await page.locator('input[type="file"]').first().setInputFiles(path.join(dir, f));
  await page.waitForTimeout(2500);
  console.log("uploaded", f);
}
await browser.close();
