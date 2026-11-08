import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/** The table of contents of /changelog: a column beside the text (xl), a drawer below. */
test.describe("changelog table of contents", { tag: "@parallel" }, () => {
  test.describe("wide", () => {
    test.use({ viewport: { width: 1440, height: 900 } });

    test("lists the majors and scrolls to a release", async ({
      loggedInPage: page,
    }) => {
      const { t } = await userFormat(page);
      await goto(page, "/changelog");
      const toc = page.getByRole("navigation", {
        name: t("changelog.toc.title"),
      });
      await expect(
        toc.getByRole("link", { name: /^ProjectForge 9$/ })
      ).toBeVisible();

      // The releases of the major in view are listed below it; the last one is far down the page.
      const releases = toc.locator("ul ul a[href^='#release-']");
      const last = releases.last();
      const target = (await last.getAttribute("href"))!.substring(1);
      await last.click();
      await expect(page.locator(`[id="${target}"]`)).toBeInViewport();
      await expect(last).toHaveAttribute("aria-current", "location");
    });

    test("the column can be hidden and shown again", async ({
      loggedInPage: page,
    }) => {
      const { t } = await userFormat(page);
      await goto(page, "/changelog");
      const toc = page.getByRole("navigation", {
        name: t("changelog.toc.title"),
      });
      await page.getByRole("button", { name: t("changelog.toc.hide") }).click();
      await expect(toc).toBeHidden();
      await page.reload();
      await page.getByRole("button", { name: t("changelog.toc.show") }).click();
      await expect(toc).toBeVisible();
    });
  });

  test.describe("narrow", () => {
    test.use({ viewport: { width: 390, height: 800 } });

    test("opens as a drawer that closes on a link", async ({
      loggedInPage: page,
    }) => {
      const { t } = await userFormat(page);
      await goto(page, "/changelog");
      await page
        .getByRole("button", { name: t("changelog.toc.title"), exact: true })
        .click();
      const drawer = page.getByRole("dialog");
      const link = drawer.getByRole("link", { name: /^ProjectForge 9$/ });
      await link.click();
      await expect(drawer).toBeHidden();
      await expect(page.locator('[id="changelog-major-9"]')).toBeInViewport();
    });
  });
});
