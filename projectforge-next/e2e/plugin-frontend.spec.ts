import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * A plugin with a frontend of its own (PluginFrontend): the bundle is not part of the Next build, Spring serves it from the
 * plugin jar and the app loads it at runtime. The CVE-Pilot is the plugin at hand; without it (not on the class path, not
 * activated) there is nothing to load and the specs skip.
 */
test.describe("plugin frontend", () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    // The manifest lists the activated plugins with a frontend, however they were activated (database or properties).
    const res = await page.request.get("/rs/pluginFrontend");
    const ids: string[] = res.ok()
      ? ((await res.json()) as { id: string }[]).map((m) => m.id)
      : [];
    test.skip(
      !ids.includes("cveradar"),
      "Plugin CVE-Pilot not activated on this instance."
    );
  });

  test("renders in the app, under the CSP, also after a reload", async ({
    loggedInPage: page,
  }) => {
    const problems: string[] = [];
    page.on("console", (m) => {
      // A refusal of the server (403 for a user without the role) is no problem of the bundle.
      if (m.type() === "error" && !/status of 40[0-9]/.test(m.text())) {
        problems.push(m.text());
      }
    });
    page.on("pageerror", (e) => problems.push(e.message));
    await page.addInitScript(() => {
      document.addEventListener("securitypolicyviolation", (e) =>
        console.error(`CSP violation: ${e.violatedDirective} ${e.blockedURI}`)
      );
    });
    const { t } = await userFormat(page);
    for (const step of ["open", "reload"]) {
      if (step === "open") await goto(page, "/plugin/cveradar/catalog");
      else await page.reload();
      // The texts are those of the app's catalog, the page and its components come from the plugin.
      await expect(
        page.getByRole("heading", { name: t("plugins.cveradar.catalog.title") })
      ).toBeVisible();
    }
    expect(problems).toEqual([]);
  });

  test("deep links and the way between the pages of a plugin: back and forward", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/plugin/cveradar/catalog");
    await page
      .getByRole("link", { name: t("plugins.cveradar.live.nav.inventory") })
      .click();
    await expect(page).toHaveURL(/\/next\/plugin\/cveradar\/inventory\/?$/);
    await page.goBack();
    await expect(page).toHaveURL(/\/next\/plugin\/cveradar\/catalog\/?$/);
    await page.goForward();
    await expect(page).toHaveURL(/\/next\/plugin\/cveradar\/inventory\/?$/);
  });

  test("an unknown page of the plugin is not found, a missing bundle is a real 404", async ({
    loggedInPage: page,
  }) => {
    const res = await page.request.get(
      "/next/plugins-assets/cveradar/missing.js"
    );
    expect(res.status()).toBe(404);
    await goto(page, "/plugin/cveradar/nothing-here");
    await expect(
      page.getByText(/404|not found|nicht gefunden/i).first()
    ).toBeVisible();
  });
});
