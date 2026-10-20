import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { writeHeaders } from "./fixtures/seed";

/**
 * The period of the vacation list, as the backend remembers it (getList → saveCurrentFilter) and the list
 * restores it on its return from an entry (useRememberedFilter).
 *
 * The regression: paging back to a filter seen a moment ago was answered from React Query's cache without
 * a request, so the backend kept the filter before, and the list came back from the entry with that one
 * (useMagicFilterQuery's `staleTime`). Writes nothing but the list filter, reset before and after.
 */
test.describe("vacation list filter", { tag: "@lane-vacation" }, () => {
  const reset = async (page: import("@playwright/test").Page) =>
    page.request
      .post("/rs/vacation/filter/reset", {
        headers: await writeHeaders(page.request),
      })
      .catch(() => undefined);

  test.beforeEach(async ({ loggedInPage: page }) => reset(page));
  test.afterEach(async ({ loggedInPage: page }) => reset(page));

  test("starts with the calendar year and keeps the year paged to across an entry", async ({
    loggedInPage: page,
  }) => {
    const { t, date } = await userFormat(page);
    const year = new Date().getFullYear();
    const yearText = (y: number) =>
      `${t("timePeriod")}: ${date(`${y}-01-01`)} – ${date(`${y}-12-31`)}`;
    await goto(page, "/vacation");
    const pill = page.getByRole("button", {
      name: t("filter.editEntry", { arg0: t("timePeriod") }),
    });
    await expect(pill).toHaveText(yearText(year));

    const step = (key: string) =>
      page
        .getByRole("button", {
          name: t(`calendar.quickselect.tooltip.${key}`),
        })
        .click();
    await step("selectPreviousCalendarYear");
    await expect(pill).toHaveText(yearText(year - 1));
    // Back to the year shown first: that answer is in the cache, and must be asked for all the same.
    const stored = page.waitForResponse(
      (r) =>
        r.url().includes("/rs/vacation/list") &&
        (r.request().postData() ?? "").includes(`"to":"${year}-12-31"`)
    );
    await step("selectNextCalendarYear");
    await expect(pill).toHaveText(yearText(year));
    await stored;

    await goto(page, "/vacation/new");
    await page.getByRole("button", { name: t("cancel"), exact: true }).click();
    await expect(page).toHaveURL(/\/vacation(\?|$)/);
    await expect(pill).toHaveText(yearText(year));
  });
});
