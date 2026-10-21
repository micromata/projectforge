import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { writeHeaders } from "./fixtures/seed";
import { formatMonthName } from "../lib/format";

/**
 * The birthday butler against the live backend (`BirthdayButlerRest`): the current month is preselected, and the
 * download either saves the Word document or — on an installation without a configured organization, like a fresh
 * slot DB — the page says so up front. Writes nothing.
 *
 * Skipped for a test user outside the ORGA team and the HR group (the page is theirs only).
 */
test.describe("birthday butler", { tag: "@parallel" }, () => {
  test("offers the current month and downloads its Word document", async ({
    loggedInPage: page,
  }) => {
    const { t, context } = await userFormat(page);
    const init = page.waitForResponse((r) =>
      r.url().includes("/rs/birthdayButler/init")
    );
    await goto(page, "/birthdayButler");
    const initResponse = await init;
    test.skip(
      initResponse.status() === 403,
      "The test user is neither in the ORGA team nor in the HR group."
    );
    await expect(
      page.getByRole("heading", { name: t("menu.birthdayButler") })
    ).toBeVisible();
    const { month, configured } = (await initResponse.json()) as {
      month: number;
      configured: boolean;
    };
    const monthName = formatMonthName(month, context);
    await expect(page.locator("#birthdayButlerMonth")).toContainText(monthName);

    const button = page.getByRole("button", { name: t("download._") });
    if (!configured) {
      await expect(
        page.getByText(t("birthdayButler.organization.notSet"))
      ).toBeVisible();
      await expect(button).toBeDisabled();
      // The endpoint itself refuses with the reason as text (what the page would show as toast).
      const refused = await page.request.post("/rs/birthdayButler/download", {
        headers: await writeHeaders(page.request),
        data: { month },
      });
      expect(refused.status()).toBe(400);
      expect(await refused.text()).toBe(
        t("birthdayButler.organization.notSet")
      );
      return;
    }
    // A month without birthdays is answered with a toast; any month with one gives the document.
    const answer = page.waitForResponse((r) =>
      r.url().includes("/rs/birthdayButler/download")
    );
    const download = page.waitForEvent("download").catch(() => null);
    await button.click();
    if ((await answer).ok()) {
      expect((await download)?.suggestedFilename()).toMatch(/\.docx$/);
    }
  });
});
