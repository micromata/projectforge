import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import type { IhkInit } from "../components/features/ihk/types";

/**
 * The IHK training report of the IHK plugin (/next/ihk, IHKRest) against the live backend.
 *
 * Read-only (the settings form is not saved, the account's prefs stay untouched). Whether the page offers the
 * download depends on the account's training settings, which a test account may or may not have: the spec reads
 * `/rs/ihk/init` first and asserts the state the page has to be in — the settings form, or the week picker with
 * the download.
 */
test.describe("ihk", { tag: "@parallel" }, () => {
  test("asks for the settings, or offers the week's report", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const response = await page.request.get("/rs/ihk/init");
    expect(response.ok()).toBe(true);
    const init = (await response.json()) as IhkInit;

    await goto(page, "/ihk");
    await expect(
      page.getByRole("heading", { name: t("plugins.ihk.title") })
    ).toBeVisible();

    if (!init.settings) {
      await expect(
        page.getByText(t("plugins.ihk.settings.notConfigured"))
      ).toBeVisible();
      await expect(
        page.getByLabel(t("plugins.ihk.settings.teamname._"))
      ).toBeVisible();
      // Without a training start, there is nothing to save yet.
      await expect(
        page.getByRole("button", { name: t("save") })
      ).toBeDisabled();
      await expect(
        page.getByRole("button", { name: t("plugins.ihk.download") })
      ).toHaveCount(0);
      return;
    }

    await expect(
      page.getByRole("button", { name: t("plugins.ihk.settings.edit") })
    ).toBeVisible();
    const next = page.getByRole("button", {
      name: t("calendar.quickselect.tooltip.selectNextWeek"),
    });
    const input = page.locator("#ihk-week");
    const before = await input.inputValue();
    await next.click();
    await expect(input).not.toHaveValue(before);
    await expect(
      page.getByRole("button", {
        name: new RegExp(
          `${t("plugins.ihk.download")}|${t("plugins.ihk.downloadAnyway")}`
        ),
      })
    ).toBeVisible();
  });
});
