import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import { createBookWithHistory, type SeededBook } from "./fixtures/seed";

/**
 * The change history, against the live backend. Read-only: it opens a history and toggles rows, it
 * never writes.
 *
 * Nothing here is book-specific — `components/shared/history/` serves every `AbstractPagesRest`
 * entity, and the endpoint is `/rs/{entity}/history/{id}` for all of them. Books are merely the
 * vehicle: today they are the only hand-built page with a history route. When a second one arrives,
 * add it to `ENTITIES` rather than copying the spec.
 *
 * The history is the test's own, produced by `createBookWithHistory`: history exists only where
 * something was written, so no row of a database can be relied on to have one — and a row of *this*
 * database is a row of production (see fixtures/seed.ts).
 */
const ENTITIES = [
  {
    name: "book",
    seed: createBookWithHistory,
    historyPath: (book: SeededBook) => `/book/${book.id}/history`,
  },
];

test.describe("change history", () => {
  let format: UserFormat;

  test.beforeEach(async ({ loggedInPage: page }) => {
    format = await userFormat(page);
  });

  for (const entity of ENTITIES) {
    // One seed per entity, not per case: the two cases only read the history they are given.
    let seeded: SeededBook;
    test.beforeAll(async ({ seedRequest }) => {
      seeded = await entity.seed(seedRequest);
    });

    test(`${entity.name}: entries are collapsed and reveal their values on demand`, async ({
      loggedInPage: page,
    }) => {
      await goto(page, entity.historyPath(seeded));

      // One row per entry, each a disclosure button. Addressed through the list item, not through
      // `{ expanded: false }`: that predicate would re-resolve to a *different* row the moment this
      // one opens.
      const first = page.getByRole("listitem").first().getByRole("button");
      await expect(first).toBeVisible();
      await expect(first).toHaveAttribute("aria-expanded", "false");

      // Collapsed rows name the changed fields, so the overview still says what happened…
      await expect(
        page.getByText(`${format.t("history.fields")}:`).first()
      ).toBeVisible();
      // …but the values are hidden, which is the point: an insert brings one attribute per property
      // and would otherwise fill the page.
      const changes = page.getByText(format.t("changes"), { exact: true });
      await expect(changes).toHaveCount(0);

      await first.click();
      await expect(first).toHaveAttribute("aria-expanded", "true");
      await expect(changes.first()).toBeVisible();

      await first.click();
      await expect(first).toHaveAttribute("aria-expanded", "false");
      await expect(changes).toHaveCount(0);
    });

    test(`${entity.name}: a row toggles with the keyboard`, async ({
      loggedInPage: page,
    }) => {
      await goto(page, entity.historyPath(seeded));
      const first = page.getByRole("listitem").first().getByRole("button");
      await expect(first).toBeVisible();

      // The legacy row was a div with tabIndex={-1} and an empty onKeyDown — not operable at all.
      await first.focus();
      await page.keyboard.press("Enter");
      await expect(first).toHaveAttribute("aria-expanded", "true");
      await page.keyboard.press(" ");
      await expect(first).toHaveAttribute("aria-expanded", "false");
    });
  }
});
