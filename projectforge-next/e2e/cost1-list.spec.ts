import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { KOST1_METADATA } from "../lib/metadata/kost1.generated";
import { COST1_PAGE } from "../components/features/cost1/cost1.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";
import type { SeededCost1 } from "./fixtures/seed";

/**
 * The cost 1 list against the live backend — the first page rendered entirely from a declaration
 * (see COST1_PAGE and components/shared/list/entity-list-page.tsx).
 *
 * Two things are under test at once: that the generic renderer produces the declared columns with the
 * labels of Kost1DO, and that the DTO carries the values at all. `rest/dto/Kost1` used to copy only
 * `copyFromMinimal`, so every row arrived with four zeros and no `formattedNumber` — hence the
 * assertion that the number cell is a *number*, not merely present.
 *
 * Read-only: nothing is written here, and the stored filter is reset before each case so a criterion
 * left behind by another run cannot empty the list under test. The row it searches for is the one
 * `seededCost1` created (see fixtures/seed.ts) — the list of this database is a production chart of
 * accounts, and a fresh one holds no cost unit at all.
 */
test.describe("cost 1 list", () => {
  let cost1: SeededCost1;

  test.beforeEach(async ({ loggedInPage: page, seededCost1 }) => {
    cost1 = seededCost1;
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
    for (const column of COST1_PAGE.columns) {
      const name = columnIdOf(column);
      const key = columnHeaderKeyOf(column, KOST1_METADATA);
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
    // guards showed every row as "0.000.00.00" or empty. Asserted on *a* row rather than on a count:
    // the seeded entry is the only one the test may rely on being there.
    const numbers = page.getByRole("cell").filter({ hasText: /^\d\.\d{3}\./ });
    await expect(numbers.first()).toBeVisible();

    // The status is an enum, so the cell must read the backend's label and not the constant.
    const { t } = await userFormat(page);
    await expect(
      page.getByRole("cell", { name: t("fibu.kost.status.active") }).first()
    ).toBeVisible();
    await expect(page.getByText("ACTIVE")).toHaveCount(0);
  });

  test("sorts by the cost number", async ({ loggedInPage: page }) => {
    const { t } = await userFormat(page);
    await goto(page, "/cost1");
    const numbers = page.getByRole("cell").filter({ hasText: /^\d\.\d{3}\./ });
    await expect(numbers.first()).toBeVisible();

    // A click on the header cell sorts (DataTable sorts on the whole cell, see
    // DataTableColumnHeader), and the sort is the backend's: `manualSorting` is on.
    await page.getByRole("columnheader", { name: t("fibu.kost1._") }).click();

    // Every part of the number is a fixed count of digits, so the formatted numbers as shown sort
    // like plain strings — which is what the four columns the backend orders by have to produce
    // (Kost1PagesRest.postProcessMagicFilter). `formattedNumber` is a getter without a column of its
    // own, so before that mapping the criteria query dropped the order and the list came back
    // unsorted.
    await expect
      .poll(
        async () => {
          const shown = await numbers.allInnerTexts();
          return shown.join() === [...shown].sort().join();
        },
        { message: "the number column must sort ascending" }
      )
      .toBe(true);
  });

  test("narrows the list by the search box", async ({ loggedInPage: page }) => {
    const { t } = await userFormat(page);
    await goto(page, "/cost1");
    const numbers = page.getByRole("cell").filter({ hasText: /^\d\.\d{3}\./ });
    // Wait for the first page before searching — read straight after the navigation the table is
    // still empty, and every statement about a narrowed list would hold vacuously.
    await expect(numbers.first()).toBeVisible();

    await page
      .getByPlaceholder(t(COST1_PAGE.searchPlaceholderKey))
      .fill(cost1.suffix);

    // The run's suffix alone, not the whole description: the rest of it is the same in every run, and
    // the backend's search matches a row on any word — the entries of earlier runs would be hits too.
    // The suffix leaves exactly one row, which says more than "fewer rows than before" and holds on a
    // database of any size.
    await expect
      .poll(() => numbers.allInnerTexts(), {
        message: "the search must leave exactly the searched row",
      })
      .toEqual([cost1.number]);
  });

  test("opens a cost unit by clicking its row", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/cost1");
    await page
      .getByPlaceholder(t(COST1_PAGE.searchPlaceholderKey))
      .fill(cost1.description);

    await page.getByRole("cell", { name: cost1.number }).click();

    await expect(page).toHaveURL(new RegExp(`/cost1/${cost1.id}$`));
    // The edit page loaded the entry the row stands for: its description is in the form.
    await expect(
      page.getByRole("textbox", { name: t("description") })
    ).toHaveValue(cost1.description);
  });
});
