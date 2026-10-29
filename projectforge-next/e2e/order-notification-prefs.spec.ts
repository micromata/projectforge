import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { writeHeaders } from "./fixtures/seed";

interface Settings {
  defaultMode: string;
  rules?: { type: string; id: number; displayName?: string; mode: string }[];
}

/**
 * The order notification settings (`MyAccountRest.orderNotificationPrefs`, edited in a dialog of the order pages): a default
 * mode and exceptions for projects and customers, saved with the dialog's Save. The settings of the logged-in test user are
 * restored at the end of every test.
 */
test.describe("order notification prefs", () => {
  let original: Settings;

  test.beforeEach(async ({ loggedInPage: page }) => {
    original = await (
      await page.request.get("/rs/myAccount/orderNotificationPrefs")
    ).json();
  });

  test.afterEach(async ({ loggedInPage: page }) => {
    await page.request.post("/rs/myAccount/orderNotificationPrefs", {
      data: { defaultMode: original.defaultMode, rules: original.rules ?? [] },
      headers: await writeHeaders(page.request),
    });
  });

  test("keeps rules of readable projects, drops forged ids", async ({
    loggedInPage: page,
    seededProject,
  }) => {
    const res = await page.request.post(
      "/rs/myAccount/orderNotificationPrefs",
      {
        data: {
          defaultMode: "DAILY",
          rules: [
            { type: "PROJECT", id: seededProject.id, mode: "MUTED" },
            { type: "PROJECT", id: 2147483000, mode: "MUTED" },
            { type: "CUSTOMER", id: 2147483000, mode: "MUTED" },
          ],
        },
        headers: await writeHeaders(page.request),
      }
    );
    expect(res.ok()).toBeTruthy();
    const saved: Settings = await res.json();
    expect(saved.defaultMode).toBe("DAILY");
    expect(saved.rules).toHaveLength(1);
    expect(saved.rules![0]).toMatchObject({
      type: "PROJECT",
      id: seededProject.id,
      mode: "MUTED",
    });
  });

  const openDialog = async (page: Page) => {
    const { t } = await userFormat(page);
    await goto(page, "/order");
    await page
      .getByRole("button", {
        name: t("fibu.auftrag.notification.prefs.title"),
      })
      .click();
    const dialog = page.getByRole("dialog");
    await dialog
      .getByRole("combobox", {
        name: t("fibu.auftrag.notification.prefs.default"),
      })
      .click();
    await page
      .getByRole("option", { name: t("fibu.auftrag.notification.mode.daily") })
      .click();
    return { t, dialog };
  };
  const storedDefault = async (page: Page) =>
    (
      (await (
        await page.request.get("/rs/myAccount/orderNotificationPrefs")
      ).json()) as Settings
    ).defaultMode;

  test("the dialog of the order list saves with Save", async ({
    loggedInPage: page,
  }) => {
    const { t, dialog } = await openDialog(page);
    expect(await storedDefault(page), "nothing is saved before Save").toBe(
      original.defaultMode
    );
    await dialog.getByRole("button", { name: t("save") }).click();
    await expect(dialog).toHaveCount(0);
    await expect.poll(() => storedDefault(page)).toBe("DAILY");
  });

  test("the dialog of the order list discards its changes with Cancel", async ({
    loggedInPage: page,
  }) => {
    const { t, dialog } = await openDialog(page);
    await dialog.getByRole("button", { name: t("cancel") }).click();
    await expect(dialog).toHaveCount(0);
    expect(await storedDefault(page)).toBe(original.defaultMode);
  });
});
