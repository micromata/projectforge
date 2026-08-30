import type { Locator, Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import {
  cancelButton,
  filterField,
  listRequest,
  openPill,
  resetFilter,
} from "./fixtures/filter-pill";
import { pickKind } from "./fixtures/period-kind";
import { periodKindOf, type PeriodKind } from "../lib/date-period";

const YEAR_TO_DATE = periodKindOf("yearToDate") as PeriodKind;

/** The books list stands in for any list with saved filters; its DATE field is `lendOutDate`. */
const ENTITY = "book";

/** Under this name the cases save, so the sweep afterwards can tell theirs from the account's own. */
const NAME = "e2e favorite modified";

// A live backend, and the first navigation to a route additionally waits for the dev server to compile
// it.
test.describe.configure({ timeout: 120_000 });

/**
 * Whether a saved filter says "there is something to save" — the asterisk beside its name.
 *
 * A page that was just loaded has nothing in its own session to compare the values against, so the
 * stored ones come along with the restored filter (`ListMetaData.filterFavorite`). Without them the
 * marker appeared on every reload, claiming a modification the user had just saved.
 *
 * The comparison stays in the client on purpose: `MagicFilter.isModified` compares entry objects and
 * `MagicFilterEntry.Value` is a Kotlin class with identity equality, so the backend's own answer would
 * always be "modified".
 */
test.describe("saved filter modification marker", () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await resetFilter(page, ENTITY);
    await dropOwnFavorites(page);
  });

  test.afterEach(async ({ loggedInPage: page }) => {
    await dropOwnFavorites(page);
    await resetFilter(page, ENTITY);
  });

  test("is silent right after a filter was saved under a name", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/book");
    await pickYearToDate(page, format);
    await saveAs(page, format, NAME);

    await expect(marker(page, format, NAME)).toHaveCount(0);
    await page.reload();
    // Still the applied favorite, and still nothing to save — although "Jahr bis heute" recomputes its
    // end while the filter is being restored. Late in a serial full run the reloaded page compiles and
    // restores under a loaded machine, so the wait is longer than the default here.
    await expect(bookmark(page, NAME)).toBeVisible({ timeout: 40_000 });
    await expect(marker(page, format, NAME)).toHaveCount(0);
  });

  test("goes away again when the change is saved into the favorite", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/book");
    await saveAs(page, format, NAME);

    // Now change what the favorite holds — the art of the period, which is the case this came from.
    await pickYearToDate(page, format);
    await expect(marker(page, format, NAME)).toHaveCount(1);

    await bookmark(page, NAME).click();
    await page
      .getByRole("dialog")
      .getByRole("button", { name: format.t("favorites.saveModification") })
      .click();
    await page.keyboard.press("Escape");
    await expect(marker(page, format, NAME)).toHaveCount(0);

    await page.reload();
    // The longer wait as above: the reload after the full run's load has to recompile and restore.
    await expect(bookmark(page, NAME)).toBeVisible({ timeout: 40_000 });
    await expect(marker(page, format, NAME)).toHaveCount(0);
  });
});

/**
 * The saved-filters trigger of the pill row. Its name is the applied favorite's once there is one, so a
 * case says which it expects — and the marker below is looked for inside it.
 */
function bookmark(page: Page, name: string): Locator {
  return page.getByRole("button", { name }).first();
}

/**
 * The asterisk on that trigger, located by its label attribute rather than by role: it is an icon inside
 * the button, so it has a name but no role of its own.
 */
function marker(page: Page, format: UserFormat, name: string): Locator {
  return bookmark(page, name).locator(
    `[aria-label="${format.t("favorites.saveModification")}"]`
  );
}

/** Gives the list's date filter a period with an art, i.e. a filter worth saving. */
async function pickYearToDate(page: Page, format: UserFormat): Promise<void> {
  const field = await filterField(
    page,
    ENTITY,
    "DATE",
    "Does BookDao still index lendOutDate?"
  );
  await openPill(page, format.t, field.label!);
  await pickKind(page, format, YEAR_TO_DATE);
  // The art applies to the list on its own; wait until it has landed, then close the popover — which
  // keeps it — so the saved-filters trigger below is uncovered.
  await listRequest(page, ENTITY);
  await page.keyboard.press("Escape");
  await expect(cancelButton(page, format.t)).toHaveCount(0);
}

/** Saves the filter the list currently uses under `name`, through the menu a user goes through. */
async function saveAs(
  page: Page,
  format: UserFormat,
  name: string
): Promise<void> {
  const { t } = format;
  await bookmark(page, t("favorites._")).click();
  const dialog = page.getByRole("dialog");
  // By role: the row's input and its button carry the same name, which is what a user reads once.
  await dialog
    .getByRole("textbox", { name: t("favorite.filter.addNew") })
    .fill(name);
  await dialog
    .getByRole("button", { name: t("favorite.filter.addNew") })
    .click();
  await expect(bookmark(page, name)).toBeVisible();
}

/** Removes what these cases saved, so a run cannot leave a favorite behind. */
async function dropOwnFavorites(page: Page): Promise<void> {
  const response = await page.request.get(`/rs/${ENTITY}/listMeta`, {
    headers: { "X-PF-Frontend": "next" },
  });
  const meta = (await response.json()) as {
    filterFavorites?: { id: number; name: string }[];
  };
  for (const favorite of meta.filterFavorites ?? []) {
    if (favorite.name !== NAME) continue;
    await page.request.get(`/rs/${ENTITY}/filter/delete?id=${favorite.id}`, {
      headers: { "X-PF-Frontend": "next" },
    });
  }
}
