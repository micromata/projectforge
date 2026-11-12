import { test, expect, goto } from "./fixtures/auth";

/**
 * PoC of runtime frontend plugins: a bundle that is not part of the Next build (served by Spring from
 * the plugin jar under /next/plugins-assets/) renders inside the host React tree.
 */
test.describe("runtime plugin PoC", () => {
  test("renders with the host's React and QueryClient, under the CSP, also after a reload", async ({
    loggedInPage: page,
  }) => {
    const problems: string[] = [];
    page.on("console", (m) => {
      if (m.type() === "error") problems.push(m.text());
    });
    page.on("pageerror", (e) => problems.push(e.message));
    await page.addInitScript(() => {
      document.addEventListener("securitypolicyviolation", (e) =>
        console.error(`CSP violation: ${e.violatedDirective} ${e.blockedURI}`)
      );
    });

    for (const step of ["open", "reload"]) {
      if (step === "open") await goto(page, "/pluginPoc");
      else await page.reload();
      await expect(page.getByTestId("plugin-poc")).toBeVisible();
      await expect(page.getByTestId("poc-react")).toContainText("true");
      await expect(page.getByTestId("poc-queryclient")).toContainText("true");
      await expect(page.getByTestId("poc-query")).toContainText("ok");
      await page.getByTestId("poc-button").click();
      await expect(page.getByTestId("poc-button")).toContainText("clicked 1");
    }
    expect(problems).toEqual([]);
  });

  test("a missing bundle is a real 404, not the app shell", async ({ request }) => {
    const res = await request.get("/next/plugins-assets/cveradar-poc/missing.js");
    expect(res.status()).toBe(404);
  });
});
