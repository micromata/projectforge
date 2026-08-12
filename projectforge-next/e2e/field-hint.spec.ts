import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";

/**
 * The explanation of a field: behind an ⓘ next to the label, not printed under the control — see
 * [FieldHint]. The order's head section is where this matters most, because two of its fields carry a
 * whole sentence each.
 *
 * Nothing is saved; the form is opened and abandoned.
 */
// More than the default 30 s, which is less than the wait below: the first navigation to `/order/new`
// waits for the dev server to compile the route, and the order form is a large one (see order.spec.ts).
test.describe.configure({ timeout: 120_000 });

test.describe("field hint", () => {
  test("keeps a field's explanation behind its ⓘ", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    // The first navigation to this route compiles it, so the form has to be there before anything of it
    // can be hovered.
    await expect(
      page.getByRole("textbox", { name: format.t("fibu.auftrag.title._") })
    ).toBeVisible({ timeout: 60_000 });

    const explanation = format.t("fibu.auftrag.forecastType.info");
    // Not on the page as ordinary text — that is the point of the change: printed under every field it
    // belongs to, the sentences push the form apart.
    await expect(page.getByText(explanation)).toHaveCount(0);

    const forecastType = label(format, "fibu.auftrag.forecastType");
    const hint = page.getByRole("button", {
      name: `${format.t("form.hint")}: ${forecastType}`,
    });
    await expect(hint).toBeVisible();
    await hint.hover();
    await expect(page.getByRole("tooltip")).toContainText(explanation);

    // The ⓘ is not part of the label: clicking it must not open the select it explains.
    await hint.click();
    await expect(
      page.getByRole("combobox", { name: forecastType })
    ).toBeVisible();
    await expect(page.getByRole("listbox")).toHaveCount(0);
  });
});
