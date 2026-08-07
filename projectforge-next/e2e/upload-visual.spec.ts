import { test, goto } from "./fixtures/auth";

const BOOK_ID = 316163;
const names = ["pf-e2e-vis-a.bin", "pf-e2e-vis-b.bin", "pf-e2e-vis-c.bin"];

test("visual: several bars", async ({ loggedInPage: page }) => {
  page.on("response", async (r) => {
    if (r.url().includes("/attachments/upload")) {
      console.log(
        "RESP",
        r.status(),
        (await r.text().catch(() => "?")).slice(0, 200)
      );
    }
  });
  page.on("console", (m) =>
    console.log("CONSOLE", m.type(), m.text().slice(0, 200))
  );
  await goto(page, `/books/${BOOK_ID}`);
  await page
    .getByLabel(/datei wählen/i)
    .setInputFiles(
      names.map((name) => ({
        name,
        mimeType: "application/octet-stream",
        buffer: Buffer.alloc(300_000, 7),
      }))
    );
  await page.waitForTimeout(8000);
  for (const name of names) {
    console.log(
      name,
      "stored:",
      await page.getByRole("link", { name: `Download: ${name}` }).count()
    );
  }
});
