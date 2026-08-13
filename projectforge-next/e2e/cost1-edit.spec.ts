import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { createCost1, uniqueSuffix, type SeededCost1 } from "./fixtures/seed";
import type { Page } from "@playwright/test";

/**
 * The cost 1 edit page against the live backend — the segmented number field and the generic edit
 * renderer (see COST1_PAGE and components/shared/edit/entity-edit-page.tsx).
 *
 * Both cost units are the tests' own (`seededCost1` and a second one created here, see
 * fixtures/seed.ts): the database is a copy of production, so no number or description of it may be
 * written into the source — and a spec needing a particular entry could not run against a fresh
 * database at all. The seeded number covers the padding cases by construction: a one-digit
 * nummernkreis that is *not* padded, plus a bereich of three and a teilbereich and endziffer of two
 * digits each.
 *
 * Two inserts per run is the price of that, and they are permanent: `Kost1DO` supports no real delete
 * (`markAsDeleted` keeps the row, and `Kost1Dao.onInsertOrModify` collides with a deleted row just as
 * with a live one), so an inserted cost number stays occupied for good. Hence the second entry is
 * created once for the whole file rather than per case, and the shared one gets its description back.
 */
test.describe("cost 1 edit", () => {
  let cost1: SeededCost1;
  /**
   * A number that is certainly taken — because this test took it.
   *
   * Deliberately not a number read off the database: if such a number turned out to be free after
   * all, the save below would *succeed* and silently renumber a production cost unit. A number the
   * test created itself cannot be free.
   */
  let taken: SeededCost1;

  test.beforeAll(async ({ seedRequest, seededCost1 }) => {
    cost1 = seededCost1;
    // A suffix of its own, not the default: the shared entry may have been created in the same
    // second, and cost1-list.spec.ts searches for exactly one row by that suffix. Prefixed rather
    // than suffixed, because the backend's search appends a wildcard — a trailing character would
    // still be found by the shared entry's term.
    taken = await createCost1(seedRequest, `taken${uniqueSuffix()}`);
  });

  test("prefills the four boxes of the stored number, padded as Wicket shows them", async ({
    loggedInPage: page,
  }) => {
    // A deep link, not a click from the list: it is what proves the SPA shell map covers
    // `cost1/[id]` — under `output: 'export'` an unknown route would land on Next's 404.
    await goto(page, `/cost1/${cost1.id}`);

    for (const [name, value] of Object.entries(cost1.parts)) {
      // Padding is display only and applies to the parts wider than one digit — the nummernkreis has
      // no converter in Wicket's form either, so a "1" stays a "1".
      await expect(await box(page, name), name).toHaveValue(value);
    }
    await expect(
      page.getByRole("textbox", {
        name: (await userFormat(page)).t("description"),
      })
    ).toHaveValue(cost1.description);
  });

  test("fills the whole number as it is typed box by box", async ({
    loggedInPage: page,
  }) => {
    await goto(page, "/cost1/new");
    await (await box(page, "nummernkreis")).click();
    // One run of digits, no separators: a full box hands the focus to the next one. Nummernkreis 9 as
    // in [createCost1], so the digits are not a number of the real chart of accounts even though
    // nothing here is ever saved.
    await page.keyboard.type("98765432");

    await expect(await box(page, "nummernkreis")).toHaveValue("9");
    await expect(await box(page, "bereich")).toHaveValue("876");
    await expect(await box(page, "teilbereich")).toHaveValue("54");
    await expect(await box(page, "endziffer")).toHaveValue("32");
  });

  test("refuses to save a number that is missing a part", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    let saveAttempted = false;
    await page.route("**/rs/cost1/saveorupdate*", (route) => {
      saveAttempted = true;
      return route.abort();
    });

    await goto(page, "/cost1/new");
    await (await box(page, "nummernkreis")).click();
    await page.keyboard.type("98765432");
    const bereich = await box(page, "bereich");
    await bereich.fill("");
    await bereich.blur();
    await page.getByRole("button", { name: t("save") }).click();

    // One complaint under the group, not four: to the user the parts are a single number, and it is
    // named by the group's own label (`fibu.kost.kostentraeger`, as Wicket's fieldset is).
    const required = t("validation.error.fieldRequired", {
      arg0: t("fibu.kost.kostentraeger"),
    });
    await expect(page.getByText(required)).toHaveCount(1);
    expect(
      saveAttempted,
      "an incomplete number must not reach the server"
    ).toBe(false);
  });

  test("shows the backend's own words when the number is already taken", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, `/cost1/${cost1.id}`);
    await expect(await box(page, "bereich")).toHaveValue(cost1.parts.bereich);

    for (const [name, value] of Object.entries(taken.parts)) {
      const input = await box(page, name);
      await input.fill(value);
      await input.blur();
    }
    await page.getByRole("button", { name: t("save") }).click();

    // `Kost1Dao.onInsertOrModify` throws a UserException, which comes back as an HTTP 406 without a
    // `causedByField` — so it belongs to the form as a whole and is shown as a toast, translated by
    // the server (see AbstractPagesRestUtils.saveOrUpdate).
    await expect(page.getByText(t("fibu.kost.error.collision"))).toBeVisible();
    // Nothing was written, so the page stays where it is; a successful save leaves for the list.
    await expect(page).toHaveURL(new RegExp(`/cost1/${cost1.id}$`));
  });

  test("saves a change and returns to the list", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const changed = `${cost1.description} [pf-e2e]`;
    await goto(page, `/cost1/${cost1.id}`);
    const description = page.getByRole("textbox", { name: t("description") });
    await expect(description).toHaveValue(cost1.description);

    try {
      await description.fill(changed);
      await page.getByRole("button", { name: t("save") }).click();

      await expect(
        page.getByText(t("message.successfullChanged"))
      ).toBeVisible();
      await expect(page).toHaveURL(/\/cost1$/);
      // Read back through the API, so the assertion is on what was stored rather than on what the
      // list happens to have cached.
      expect(await storedDescription(page, cost1.id)).toBe(changed);
    } finally {
      // Back to what the fixture handed over: the entry is shared with cost1-list.spec.ts.
      await restoreDescription(page, cost1.id, cost1.description);
    }
    expect(await storedDescription(page, cost1.id)).toBe(cost1.description);
  });

  test("cancelling the delete question writes nothing", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    let deleteAttempted = false;
    await page.route("**/rs/cost1/markAsDeleted*", (route) => {
      deleteAttempted = true;
      return route.abort();
    });

    await goto(page, `/cost1/${cost1.id}`);
    await page.getByRole("button", { name: t("markAsDeleted") }).click();
    await expect(
      page.getByText(t("question.markAsDeletedQuestion"))
    ).toBeVisible();
    await page.getByRole("button", { name: t("cancel") }).click();

    await expect(
      page.getByText(t("question.markAsDeletedQuestion"))
    ).toHaveCount(0);
    await expect(page).toHaveURL(new RegExp(`/cost1/${cost1.id}$`));
    expect(deleteAttempted, "cancelling must not delete").toBe(false);
  });
});

/**
 * One box of the number, addressed by its accessible name — the group's label plus the part's own
 * (see SegmentedNumberField), both from the bundle rather than spelled out here.
 */
async function box(page: Page, name: string) {
  const { t } = await userFormat(page);
  return page.getByRole("textbox", {
    name: `${t("fibu.kost.kostentraeger")}: ${t(`fibu.kost1.${name}`)}`,
    exact: true,
  });
}

async function storedDescription(
  page: Page,
  id: number
): Promise<string | undefined> {
  const response = await page.request.get(`/rs/cost1/${id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  return ((await response.json()) as { description?: string }).description;
}

/** Writes the description straight through the API, bypassing the page under test. */
async function restoreDescription(page: Page, id: number, description: string) {
  if ((await storedDescription(page, id)) === description) return;
  const status = await page.request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  const current = await page.request.get(`/rs/cost1/${id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  await page.request.put("/rs/cost1/saveorupdate", {
    headers: { "X-PF-Frontend": "next", "X-PF-CSRF-Token": csrfToken },
    data: { data: { ...(await current.json()), description } },
  });
}
