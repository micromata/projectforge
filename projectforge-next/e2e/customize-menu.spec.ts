import type { Locator, Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { uniqueSuffix, writeHeaders } from "./fixtures/seed";
import type { MenuData, MenuItem } from "../lib/rs/types";

/**
 * "Customize my menu" against the live backend (MenuCustomizerRest). The favorites menu is the user's own
 * pref, which every page's favorites bar shows, so this runs alone; the writing test stores the menu it
 * found again at its end.
 */
test.describe("customize menu", { tag: "@isolated" }, () => {
  test("adds by click and undoes it", async ({ loggedInPage: page }) => {
    const { t } = await userFormat(page);
    await goto(page, "/customizeMenu");
    await expect(
      page.getByRole("heading", { name: t("menu.customizer.title") })
    ).toBeVisible();

    const entry = page.getByTestId("template-entry").first();
    const title = (await entry.textContent())?.trim() ?? "";
    const custom = page.getByTestId("custom-menu");
    await expect(custom.getByText(title, { exact: true })).toHaveCount(0);
    await entry.click();
    await expect(custom.getByText(title, { exact: true })).toBeVisible();

    await page.getByRole("button", { name: t("menu.customizer.undo") }).click();
    await expect(custom.getByText(title, { exact: true })).toHaveCount(0);
  });

  test("stores a new group with an entry dragged into it", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const original = await favorites(page);
    try {
      await goto(page, "/customizeMenu");
      const name = `E2E ${uniqueSuffix()}`;
      await page
        .getByRole("button", { name: t("menu.customizer.addGroup") })
        .click();
      const input = page.getByRole("textbox", {
        name: t("menu.customizer.groupName"),
      });
      await input.fill(name);
      await input.press("Enter");
      const group = page
        .getByTestId("custom-menu-group")
        .filter({ hasText: name });
      await expect(group).toContainText(t("menu.customizer.dropItemsHere"));

      const entry = page.getByTestId("template-entry").first();
      const title = (await entry.textContent())?.trim() ?? "";
      await drag(
        page,
        entry,
        group.getByText(t("menu.customizer.dropItemsHere"))
      );
      await expect(group.getByText(title, { exact: true })).toBeVisible();

      await page
        .getByRole("button", { name: t("menu.customizer.saveChanges") })
        .click();
      await expect(
        page.getByText(t("menu.customizer.menuSavedSuccessfully"))
      ).toBeVisible();
      const stored = (await favorites(page)).find(
        (item) => item.title === name
      );
      expect(stored?.subMenu?.map((sub) => sub.title)).toEqual([title]);
    } finally {
      await page.request.post("/rs/menu/customized", {
        headers: await writeHeaders(page.request),
        data: { favoritesMenu: original },
      });
    }
  });
});

async function favorites(page: Page): Promise<MenuItem[]> {
  const response = await page.request.get("/rs/menu", {
    headers: { "X-PF-Frontend": "next" },
  });
  return ((await response.json()) as MenuData).favoritesMenu.menuItems;
}

/** A pointer drag, in steps: dnd-kit's pointer sensor starts only after a few pixels of movement. */
async function drag(page: Page, source: Locator, target: Locator) {
  const from = await source.boundingBox();
  const to = await target.boundingBox();
  if (!from || !to) throw new Error("Drag source or target not visible.");
  await page.mouse.move(from.x + from.width / 2, from.y + from.height / 2);
  await page.mouse.down();
  await page.mouse.move(
    from.x + from.width / 2 + 10,
    from.y + from.height / 2,
    {
      steps: 5,
    }
  );
  await page.mouse.move(to.x + to.width / 2, to.y + to.height / 2, {
    steps: 15,
  });
  await page.mouse.up();
  // dnd-kit swallows every click for 50 ms after a drop (the pointer sensor's click guard), so a click
  // right after this one — no person is that fast — would be lost.
  await page.waitForTimeout(100);
}
