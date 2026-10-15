import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The chart dashboard (components/shared/dashboard/), on the liquidity forecast: a tile's width is changed
 * in edit mode, survives a reload (stored per user through `/rs/uiSettings/dashboard/{id}`), and "Reset
 * layout" brings the defaults back. Order, heights and hiding share the very plumbing.
 *
 * `@parallel`: the only state touched is this dashboard's layout, which nothing else asserts on, and the
 * test resets it at the end.
 */
test.describe("chart dashboard", { tag: "@parallel" }, () => {
  test("stores a tile's width and resets the layout", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const openForecast = async () => {
      await goto(page, "/liquidity");
      await page
        .getByRole("tab", {
          name: format.t("plugins.liquidityplanning.forecast._"),
        })
        .click();
    };
    const balance = page.locator("[data-slot=card]").filter({
      hasText: format.t("plugins.liquidityplanning.forecast.balance"),
    });
    /** The balance tile's share of the dashboard grid's width. */
    const balanceShare = async () => {
      const tile = await balance.boundingBox();
      const grid = await balance.locator("..").boundingBox();
      return tile!.width / grid!.width;
    };
    const saved = () =>
      page.waitForResponse(
        (response) =>
          response.request().method() === "POST" &&
          response.url().includes("/rs/uiSettings/dashboard/liquidity.forecast")
      );
    const arrange = () =>
      page
        .getByRole("button", { name: format.t("dashboard.arrange._") })
        .click();

    await openForecast();
    await expect(balance).toBeVisible();
    // Starts from the defaults: full width.
    await resetIfArranged(page, format.t);
    expect(await balanceShare()).toBeGreaterThan(0.9);

    await arrange();
    await balance
      .getByRole("button", { name: format.t("dashboard.tileMenu") })
      .click();
    const stored = saved();
    await page
      .getByRole("menuitemradio", { name: format.t("dashboard.width.half") })
      .click();
    await stored;
    expect(await balanceShare()).toBeLessThan(0.6);
    await page
      .getByRole("button", { name: format.t("dashboard.done") })
      .click();

    await page.reload();
    await openForecast();
    await expect(balance).toBeVisible();
    expect(await balanceShare()).toBeLessThan(0.6);

    await arrange();
    const reset = saved();
    await page
      .getByRole("button", { name: format.t("dashboard.reset") })
      .click();
    await reset;
    expect(await balanceShare()).toBeGreaterThan(0.9);
  });
});

/** Leaves the dashboard on its defaults, should an earlier, aborted run have left an arrangement behind. */
async function resetIfArranged(page: Page, t: (key: string) => string) {
  await page.getByRole("button", { name: t("dashboard.arrange._") }).click();
  const reset = page.getByRole("button", { name: t("dashboard.reset") });
  if (await reset.isEnabled()) {
    const stored = page.waitForResponse((response) =>
      response.url().includes("/rs/uiSettings/dashboard/")
    );
    await reset.click();
    await stored;
  }
  await page.getByRole("button", { name: t("dashboard.done") }).click();
}
