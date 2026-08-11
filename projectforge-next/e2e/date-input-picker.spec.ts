import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * Picking a date from the calendar of the shared date input, on a form field rather than a filter
 * pill: an order's "Bindungsfrist", which a new order leaves empty.
 *
 * Nothing is saved — the form is filled and abandoned.
 */
test.describe("date input calendar", () => {
  test("takes the first clicked day, without a second attempt", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    const field = page.getByRole("textbox", {
      name: format.t("fibu.auftrag.bindungsFrist"),
    });
    await expect(field).toBeVisible();
    await expect(field).toHaveValue("");

    // Focusing opens the calendar, which is where the bug was: the first click on a day counted as an
    // interaction outside the popover and only closed it, so the date needed a second attempt.
    await field.click();
    const grid = page.getByRole("grid");
    await expect(grid).toBeVisible();
    // A day of the month shown. Taken positionally rather than by its label, which the picker writes
    // in the user's own locale — the point here is the click, not the wording.
    await grid.getByRole("gridcell").nth(20).click();

    await expect(field).not.toHaveValue("");
  });
});
