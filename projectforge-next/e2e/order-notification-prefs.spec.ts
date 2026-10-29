import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { writeHeaders } from "./fixtures/seed";

interface Settings {
  defaultMode: string;
  rules?: { type: string; id: number; displayName?: string; mode: string }[];
}

/**
 * The order notification settings of the my-account page (`MyAccountRest.orderNotificationPrefs`): a default
 * mode and exceptions for projects and customers, saved at once. The settings of the logged-in test user are
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

  test("a change on the page is saved and survives a reload", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/myAccount");
    await page
      .getByRole("tab", { name: t("fibu.auftrag.notification.prefs.title") })
      .click();
    await page
      .getByRole("combobox", {
        name: t("fibu.auftrag.notification.prefs.default"),
      })
      .click();
    await page
      .getByRole("option", { name: t("fibu.auftrag.notification.mode.muted") })
      .click();
    await expect
      .poll(async () => {
        const s: Settings = await (
          await page.request.get("/rs/myAccount/orderNotificationPrefs")
        ).json();
        return s.defaultMode;
      })
      .toBe("MUTED");
  });
});
