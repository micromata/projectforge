import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The quick view of a colleague's availability (AvailabilityQuickViewDialog, VacationExportRest.userView), opened
 * from the vacation account: no list visited, nothing stored (the quick view leaves the team calendar's choice
 * alone), so it may run beside anything. The logged-in user picks themself, the one person always pickable.
 */
test.describe("availability quick view", { tag: "@parallel" }, () => {
  test("shows the bars of the picked person for the weeks chosen", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { username, fullname } = await userData(page);
    await goto(page, "/vacationAccount");

    await page
      .getByRole("button", { name: format.t("vacation.quickView.button") })
      .click();
    const dialog = page.getByRole("dialog", {
      name: format.t("vacation.quickView.title"),
    });
    await expect(dialog).toBeVisible();
    // The picker opens at once, its search taking the keys.
    await page.keyboard.type(username);
    await page
      .getByRole("option", { name: fullname ?? username })
      .first()
      .click();

    const chart = dialog.getByRole("group", {
      name: format.t("vacation.quickView.title"),
    });
    await expect(chart).toBeVisible();
    await expect(chart.getByText(fullname ?? username)).toBeVisible();

    // 8 weeks first; 4 asked for anew, from the beginning of this week.
    const answer = page.waitForResponse(
      (r) =>
        r.url().includes("/rs/vacationExport/userView") &&
        r.url().includes("weeks=4") &&
        r.ok()
    );
    await dialog.getByRole("radio", { name: "4" }).click();
    const view = (await (await answer).json()) as { days: unknown[] };
    expect(view.days).toHaveLength(28);
    await expect(chart).toBeVisible();
  });
});

async function userData(
  page: Page
): Promise<{ username: string; fullname?: string }> {
  const response = await page.request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  return (
    (await response.json()) as {
      userData: { username: string; fullname?: string };
    }
  ).userData;
}
