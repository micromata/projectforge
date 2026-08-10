import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import type { Page } from "@playwright/test";

/**
 * The cost 1 edit page against the live backend — the segmented number field and the generic edit
 * renderer (see COST1_PAGE and components/shared/edit/entity-edit-page.tsx).
 *
 * Cost unit 8692225 (1.007.04.00, "Incentive (Sonstiges)") is existing demo data whose number covers
 * the padding cases at once: a one-digit nummernkreis that is *not* padded, a bereich shown as "007"
 * and a teilbereich as "04".
 *
 * Only one case writes, and it writes the entry's description and puts the old one back. There is no
 * insert here on purpose: `Kost1DO` supports no real delete (`markAsDeleted` keeps the row, and
 * `Kost1Dao.onInsertOrModify` collides with a deleted row just as with a live one), so every inserted
 * cost number would stay occupied for good — the insert path is verified against the REST endpoint
 * instead, where the record can be cleaned up by the person running it.
 */
const COST1 = {
  id: 8692225,
  number: "1.007.04.00",
  description: "Incentive (Sonstiges)",
  parts: {
    nummernkreis: "1",
    bereich: "007",
    teilbereich: "04",
    endziffer: "00",
  },
};

/** A number of the demo data that is taken, so saving it onto another entry must be refused. */
const TAKEN = {
  nummernkreis: "3",
  bereich: "176",
  teilbereich: "00",
  endziffer: "00",
};

test.describe("cost 1 edit", () => {
  test("prefills the four boxes of the stored number, padded as Wicket shows them", async ({
    loggedInPage: page,
  }) => {
    // A deep link, not a click from the list: it is what proves the SPA shell map covers
    // `cost1/[id]` — under `output: 'export'` an unknown route would land on Next's 404.
    await goto(page, `/cost1/${COST1.id}`);

    for (const [name, value] of Object.entries(COST1.parts)) {
      // Padding is display only and applies to the parts wider than one digit — the nummernkreis has
      // no converter in Wicket's form either, so a "1" stays a "1".
      await expect(await box(page, name), name).toHaveValue(value);
    }
    await expect(
      page.getByRole("textbox", {
        name: (await userFormat(page)).t("description"),
      })
    ).toHaveValue(COST1.description);
  });

  test("fills the whole number as it is typed box by box", async ({
    loggedInPage: page,
  }) => {
    await goto(page, "/cost1/new");
    await (await box(page, "nummernkreis")).click();
    // One run of digits, no separators: a full box hands the focus to the next one.
    await page.keyboard.type("61000102");

    await expect(await box(page, "nummernkreis")).toHaveValue("6");
    await expect(await box(page, "bereich")).toHaveValue("100");
    await expect(await box(page, "teilbereich")).toHaveValue("01");
    await expect(await box(page, "endziffer")).toHaveValue("02");
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
    await page.keyboard.type("61000102");
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
    await goto(page, `/cost1/${COST1.id}`);
    await expect(await box(page, "bereich")).toHaveValue(COST1.parts.bereich);

    for (const [name, value] of Object.entries(TAKEN)) {
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
    await expect(page).toHaveURL(new RegExp(`/cost1/${COST1.id}$`));
  });

  test("saves a change and returns to the list", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const changed = `${COST1.description} [pf-e2e]`;
    await goto(page, `/cost1/${COST1.id}`);
    const description = page.getByRole("textbox", { name: t("description") });
    await expect(description).toHaveValue(COST1.description);

    try {
      await description.fill(changed);
      await page.getByRole("button", { name: t("save") }).click();

      await expect(
        page.getByText(t("message.successfullChanged"))
      ).toBeVisible();
      await expect(page).toHaveURL(/\/cost1$/);
      // Read back through the API, so the assertion is on what was stored rather than on what the
      // list happens to have cached.
      expect(await storedDescription(page)).toBe(changed);
    } finally {
      // Put the demo data back whatever happened above — the database is real.
      await restoreDescription(page, COST1.description);
    }
    expect(await storedDescription(page)).toBe(COST1.description);
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

    await goto(page, `/cost1/${COST1.id}`);
    await page.getByRole("button", { name: t("markAsDeleted") }).click();
    await expect(
      page.getByText(t("question.markAsDeletedQuestion"))
    ).toBeVisible();
    await page.getByRole("button", { name: t("cancel") }).click();

    await expect(
      page.getByText(t("question.markAsDeletedQuestion"))
    ).toHaveCount(0);
    await expect(page).toHaveURL(new RegExp(`/cost1/${COST1.id}$`));
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

async function storedDescription(page: Page): Promise<string | undefined> {
  const response = await page.request.get(`/rs/cost1/${COST1.id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  return ((await response.json()) as { description?: string }).description;
}

/** Writes the description straight through the API, bypassing the page under test. */
async function restoreDescription(page: Page, description: string) {
  if ((await storedDescription(page)) === description) return;
  const status = await page.request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  const current = await page.request.get(`/rs/cost1/${COST1.id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  await page.request.put("/rs/cost1/saveorupdate", {
    headers: { "X-PF-Frontend": "next", "X-PF-CSRF-Token": csrfToken },
    data: { data: { ...(await current.json()), description } },
  });
}
