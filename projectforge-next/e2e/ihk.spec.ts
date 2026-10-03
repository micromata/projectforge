import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import type { IhkInit } from "../components/features/ihk/types";

/**
 * The IHK training report of the IHK plugin (/next/ihk, IHKRest) against the live backend.
 *
 * Read-only. Whether the page offers the download depends on the account's own address (its comment must hold
 * the training settings), which a test account may or may not have: the spec reads `/rs/ihk/init` first and
 * asserts the state the page has to be in — the setup instructions, or the week picker with the download.
 */
test.describe("ihk", { tag: "@parallel" }, () => {
  test("explains the setup, or offers the week's report", async ({
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

    if (init.settingsError) {
      await expect(page.getByText(t("plugins.ihk.setup.title"))).toBeVisible();
      await expect(page.getByText(t("plugins.ihk.setup.steps"))).toBeVisible();
      // The JSON to paste into the address comment.
      await expect(page.locator("pre")).toContainText('"ausbildungsbeginn"');
      await expect(
        page.getByRole("button", { name: t("plugins.ihk.download") })
      ).toHaveCount(0);
      return;
    }

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
