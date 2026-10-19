import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { userFormat } from "./fixtures/format";

/**
 * The cost search against the live backend (see CostSearchRest): the page, and the same lookup floating over
 * other pages. Reads only: the cost unit looked up is the seeded one, as the database is a copy of production.
 * The panel's open state lives in the browser's local storage, which each test starts without.
 */
test.describe("cost search", { tag: "@lane-cost1" }, () => {
  test("looks a cost 1 unit up to its number and long name", async ({
    loggedInPage: page,
    seededCost1,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/costSearch");
    await expect(
      page.getByRole("heading", { name: t("menu.fibu.kostSearch") })
    ).toBeVisible();

    await page.getByRole("combobox", { name: t("fibu.kost1._") }).click();
    const popover = page.locator('[data-slot="popover-content"]');
    await popover.getByPlaceholder(t("filter.search")).fill(seededCost1.suffix);
    await popover
      .getByRole("option", { name: new RegExp(seededCost1.suffix) })
      .click();

    // The raw number (digits only) and the long name, number included.
    const number = page.getByRole("group", {
      name: `${t("fibu.kost1._")}: ${t("fibu.kostSearch.number")}`,
    });
    await expect(number).toHaveText(seededCost1.number.replaceAll(".", ""));
    const name = page.getByRole("group", {
      name: `${t("fibu.kost1._")}: ${t("name")}`,
    });
    await expect(name).toContainText(seededCost1.number);
    await expect(name).toContainText(seededCost1.description);
  });

  test("floats over other pages and stays open across a reload", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book");
    const panel = page.getByTestId("cost-search-panel");
    await expect(page.getByTestId("cost-search-toggle")).toBeVisible();
    await expect(panel).toHaveCount(0);

    await page.getByTestId("cost-search-toggle").click();
    await expect(panel).toBeVisible();
    await expect(
      panel.getByRole("combobox", { name: t("fibu.kost2._") })
    ).toBeVisible();

    await page.reload();
    await expect(panel).toBeVisible();

    // Collapsed to its title bar, then closed; the shortcut brings it back.
    await panel.getByRole("button", { name: t("collapse") }).click();
    await expect(
      panel.getByRole("combobox", { name: t("fibu.kost2._") })
    ).toBeHidden();
    await panel.getByRole("button", { name: t("close") }).click();
    await expect(panel).toHaveCount(0);
    await page.keyboard.press("Alt+KeyK");
    await expect(panel).toBeVisible();
  });

  test("docks the page into the panel, on the start page when opened from outside", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    // A full page load, as from the legacy React menu: going back would leave this app.
    await goto(page, "/costSearch");
    await page.getByRole("button", { name: t("fibu.kostSearch.dock") }).click();
    await expect(page).toHaveURL(/\/next\/?$/);
    await expect(page.getByTestId("cost-search-panel")).toBeVisible();
  });

  test("its menu entry opens the panel over the current page", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book");
    const field = page.getByPlaceholder(t("menu.quickAccess.placeholder"));
    await expect(async () => {
      await page.keyboard.press("ControlOrMeta+k");
      await expect(field).toBeVisible({ timeout: 1000 });
    }).toPass({ timeout: 30_000 });
    await field.fill(t("menu.fibu.kostSearch"));
    await page
      .getByRole("option", { name: t("menu.fibu.kostSearch"), exact: true })
      .click();
    await expect(page.getByTestId("cost-search-panel")).toBeVisible();
    await expect(page).toHaveURL(/\/next\/book$/);
  });

  test("is not offered without access to the cost units", async ({ page }) => {
    test.skip(!hasRole("normalo-user"), "no normalo account on this instance");
    await login(page, "normalo-user");
    // The button follows the menu (useCostSearchAccess): only its absence after the menu arrived says anything.
    const menu = page.waitForResponse(
      (response) => new URL(response.url()).pathname === "/rs/menu"
    );
    await goto(page, "/book");
    await menu;
    await expect(page.getByTestId("notification-bell")).toBeVisible();
    await expect(page.getByTestId("cost-search-toggle")).toHaveCount(0);
  });
});
