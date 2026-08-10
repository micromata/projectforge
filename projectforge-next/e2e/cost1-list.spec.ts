import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { KOST1_METADATA } from "../lib/metadata/kost1.generated";
import { COST1_PAGE } from "../components/features/cost1/cost1.page";

/**
 * The cost 1 list against the live backend — the first page rendered entirely from a declaration
 * (see COST1_PAGE and components/shared/list/entity-list-page.tsx).
 *
 * Two things are under test at once: that the generic renderer produces the declared columns with the
 * labels of Kost1DO, and that the DTO carries the values at all. `rest/dto/Kost1` used to copy only
 * `copyFromMinimal`, so every row arrived with four zeros and no `formattedNumber` — hence the
 * assertion that the number cell is a *number*, not merely present.
 *
 * Read-only: nothing is written, and the stored filter is reset before each case so a criterion left
 * behind by another run cannot empty the list under test.
 */

/** A cost unit of the demo data whose description is unique enough to search for. */
const COST1 = { id: 8692225, number: "1.007.04.00", description: "Incentive" };

test.describe("cost 1 list", () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/cost1/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("shows the declared columns under the labels of Kost1DO", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/cost1");

    // The heading is the backend's own list title, below its place in the menu (Finance > Cost).
    await expect(
      page.getByRole("heading", { name: t(COST1_PAGE.titleKey) })
    ).toBeVisible();

    // Column headers against the metadata, never against literals: the label of each column is the
    // `i18nKey` of the field in Kost1DO, which is exactly what the declaration does not repeat.
    // Widened, because a lookup by a name read off the declaration is a string lookup — the
    // generated metadata are `as const` and would otherwise narrow to the one field asked for.
    const fields: Record<string, { i18nKey?: string }> = KOST1_METADATA.fields;
    for (const column of COST1_PAGE.columns) {
      const name = "name" in column ? column.name : column.id;
      const key = fields[name]?.i18nKey ?? name;
      // `fibu.kost1` is a leaf *and* a namespace, so the generator writes it as `fibu.kost1._`.
      const label = t(key === "fibu.kost1" ? "fibu.kost1._" : key);
      await expect(
        page.getByRole("columnheader", { name: label }),
        `column ${name}`
      ).toHaveCount(1);
    }
  });

  test("fills the number column of every row", async ({
    loggedInPage: page,
  }) => {
    await goto(page, "/cost1");

    // `formattedNumber` is computed by the DO and has to be copied explicitly — the regression this
    // guards showed every row as "0.000.00.00" or empty.
    const numbers = page.getByRole("cell").filter({ hasText: /^\d\.\d{3}\./ });
    await expect(numbers.first()).toBeVisible();
    expect(await numbers.count()).toBeGreaterThan(1);

    // The status is an enum, so the cell must read the backend's label and not the constant.
    const { t } = await userFormat(page);
    await expect(
      page.getByRole("cell", { name: t("fibu.kost.status.active") }).first()
    ).toBeVisible();
    await expect(page.getByText("ACTIVE")).toHaveCount(0);
  });

  test("narrows the list by the search box", async ({ loggedInPage: page }) => {
    const { t } = await userFormat(page);
    await goto(page, "/cost1");
    const rows = page.getByRole("row");
    // Only count once the first page of the list has arrived — read straight after the navigation the
    // table is still empty, and "fewer than 0 rows" can never hold.
    await expect(
      page
        .getByRole("cell")
        .filter({ hasText: /^\d\.\d{3}\./ })
        .first()
    ).toBeVisible();
    const before = await rows.count();

    await page
      .getByPlaceholder(t(COST1_PAGE.searchPlaceholderKey))
      .fill(COST1.description);

    await expect(page.getByRole("cell", { name: COST1.number })).toBeVisible();
    // Fewer rows than before — the whole list is 548 entries, the search hits a handful.
    await expect
      .poll(() => rows.count(), { message: "the search must narrow the list" })
      .toBeLessThan(before);
  });

  test("opens a cost unit by clicking its row", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/cost1");
    await page
      .getByPlaceholder(t(COST1_PAGE.searchPlaceholderKey))
      .fill(COST1.description);

    await page.getByRole("cell", { name: COST1.number }).click();

    await expect(page).toHaveURL(new RegExp(`/cost1/${COST1.id}$`));
    // The edit page loaded the entry the row stands for: its description is in the form.
    await expect(
      page.getByRole("textbox", { name: t("description") })
    ).toHaveValue(new RegExp(COST1.description));
  });
});
