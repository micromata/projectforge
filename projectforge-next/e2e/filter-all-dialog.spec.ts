import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import type { FilterElement } from "../lib/rs/types";

/**
 * The grouped "all filters" dialog of the next lists, against the live backend.
 *
 * What it guards is the pairing of the two halves this feature has: the backend sends `group` and
 * `shortLabel` per filter field ([UIFilterElement]) and the dialog turns them into collapsed
 * sections. Neither half can be checked alone — a client-side test would have to invent the
 * metadata, and a `listMeta` assertion says nothing about what a user can reach. So the group names
 * here are read from `listMeta` rather than spelled out, which also keeps the test honest for a
 * non-German account.
 *
 * Read-only: the dialog is opened and searched, never applied, so the account's stored filter is
 * left alone.
 */
test.describe("all-filters dialog", () => {
  // The filter is stored per user and per entity, and an applied field moves out of its group into
  // "active filters" — so a criterion left behind by another run decides whether the group under test
  // exists at all. Resetting is what makes these cases repeatable.
  test.beforeEach(async ({ loggedInPage: page }) => {
    for (const entity of ["order", "book"]) {
      await page.request
        .get(`/rs/${entity}/filter/reset`, {
          headers: { "X-PF-Frontend": "next" },
        })
        .catch(() => undefined);
    }
  });

  test("groups an order's nested fields under headings from the backend", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const elements = await filterElements(page, "order");
    const group = firstGroup(elements);

    await goto(page, "/order");
    await openDialog(page, format);

    // Radix portals and focus-traps the dialog — the popover this used to live in was clipped at the
    // viewport edge, which is what made the panel look cut off.
    const dialog = page.getByRole("dialog");
    await expect(dialog).toBeVisible();

    // A nested group starts closed: its heading is there, the field behind it is not.
    const heading = dialog.getByRole("button", {
      name: new RegExp(escape(group.label)),
    });
    await expect(heading).toBeVisible();
    const field = dialog.getByText(group.shortLabel, { exact: true });
    await expect(field).toHaveCount(0);

    // And the label inside the group is the leaf alone — the heading carries the parents.
    await heading.click();
    await expect(field.first()).toBeVisible();
  });

  test("narrows to the searched group and opens it", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const elements = await filterElements(page, "order");
    const group = firstGroup(elements);

    await goto(page, "/order");
    await openDialog(page, format);
    const dialog = page.getByRole("dialog");

    await dialog.getByLabel(format.t("filter.search")).fill(group.label);

    // Only the searched group is left, and a search opens what it leaves standing: no click needed.
    await expect(
      dialog.getByRole("button", { name: new RegExp(escape(group.label)) })
    ).toHaveCount(1);
    await expect(
      dialog.getByText(group.shortLabel, { exact: true }).first()
    ).toBeVisible();
    // A field of the entity itself is a different group and drops out.
    await expect(
      dialog.getByText(ungrouped(elements, group.label), { exact: true })
    ).toHaveCount(0);
  });

  test("keeps the index-only fields in a group of their own, closed", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/book");
    await openDialog(page, format);
    const dialog = page.getByRole("dialog");

    // `attachmentsIds`/`attachmentsNames` have no @PropertyInfo, so they arrive untranslated and
    // used to sit among the real fields under their raw property name.
    const heading = dialog.getByRole("button", {
      name: new RegExp(escape(format.t("filter.moreFields"))),
    });
    await expect(heading).toBeVisible();
    await expect(dialog.getByText("attachmentsIds")).toHaveCount(0);
    await heading.click();
    await expect(dialog.getByText("attachmentsIds")).toBeVisible();
  });
});

/** The picker's "+" chip, then its "all filters" entry — the one way in. */
async function openDialog(page: Page, format: UserFormat) {
  await page.getByRole("button", { name: format.t("filter.addField") }).click();
  await page
    .getByRole("button", { name: format.t("filter.allFilters"), exact: true })
    .click();
}

async function filterElements(
  page: Page,
  entity: string
): Promise<FilterElement[]> {
  const res = await page.request.get(`/rs/${entity}/listMeta`, {
    headers: { "X-PF-Frontend": "next" },
  });
  const meta = (await res.json()) as { filterElements?: FilterElement[] };
  return meta.filterElements ?? [];
}

/**
 * The first grouped field of the list, as the backend orders them. Taken from the response rather
 * than named here: which nested entities a list offers follows from its DAO's search fields.
 */
function firstGroup(elements: FilterElement[]): {
  label: string;
  shortLabel: string;
} {
  const element = elements.find((e) => e.group && e.shortLabel);
  if (!element) {
    throw new Error(
      "No filter field with a group. Does LayoutListFilterUtils still set UIFilterElement.group?"
    );
  }
  return { label: element.group!, shortLabel: element.shortLabel! };
}

/**
 * The label of a field of the entity itself — one the searched group must not keep. Its own label,
 * not a translated key: an ungrouped field is only labelled by what `listMeta` sends.
 */
function ungrouped(elements: FilterElement[], notMatching: string): string {
  const element = elements.find(
    (e) =>
      !e.group &&
      !e.technical &&
      e.label &&
      !e.label.toLowerCase().includes(notMatching.toLowerCase())
  );
  if (!element?.label) {
    throw new Error("No ungrouped filter field to check the search against.");
  }
  return element.label;
}

/** A backend label goes into a RegExp, and "Ansprechpartner:in" is not a pattern. */
function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
