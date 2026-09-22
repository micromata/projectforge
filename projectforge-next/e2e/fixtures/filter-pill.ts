import { expect, type Locator, type Page } from "@playwright/test";
import type { FilterElement } from "../../lib/rs/types";
import type { UserFormat } from "./format";

/**
 * The filter pills of a list page, as a spec drives them: which fields a list offers, how a pill is
 * opened, and what the list request carries afterwards.
 *
 * Shared because the pills are the same control on every list, and because the fields must not be
 * spelled out: which ones exist follows from the entity's DAO and their labels are the backend's, so a
 * spec asks `listMeta` (see `LayoutListFilterUtils.createNamedSearchFilterContainer`).
 */

/** The message lookup of [userFormat] — the only source of expected texts. */
type Translate = UserFormat["t"];

/** The filter fields the list offers, in the order the backend derived them. */
export async function filterElements(
  page: Page,
  entity: string
): Promise<FilterElement[]> {
  const response = await page.request.get(`/rs/${entity}/listMeta`, {
    headers: { "X-PF-Frontend": "next" },
  });
  const meta = (await response.json()) as { filterElements?: FilterElement[] };
  return meta.filterElements ?? [];
}

/**
 * The list's first field of `filterType` a user would recognise: labelled, ungrouped and not one of the
 * technical fields an entity indexes without declaring (see `FilterElement.technical`).
 */
export async function filterField(
  page: Page,
  entity: string,
  filterType: FilterElement["filterType"],
  hint: string
): Promise<FilterElement> {
  const field = (await filterElements(page, entity)).find(
    (element) =>
      element.filterType === filterType &&
      element.label &&
      !element.group &&
      !element.technical
  );
  if (!field)
    throw new Error(`No ${filterType} filter field on ${entity}. ${hint}`);
  return field;
}

/** One end of a range filter, labelled as [RangeField] labels it. */
export function bound(
  page: Page,
  format: UserFormat,
  field: FilterElement,
  part: "value" | "valueTo"
): Locator {
  return page.getByRole("textbox", {
    name: `${field.label}: ${format.t(`filter.${part}`)}`,
  });
}

/**
 * The pill popover applies live and has no save button; "Abbrechen" is the footer control a spec
 * waits on to know the popover is open.
 */
export function cancelButton(page: Page, t: Translate): Locator {
  return page.getByRole("button", { name: t("cancel"), exact: true });
}

/** Adds a filter pill from the "+" chip and waits for its popover. */
export async function openPill(
  page: Page,
  t: Translate,
  name: string
): Promise<void> {
  await page.getByRole("button", { name: t("filter.addField") }).click();
  await page.getByRole("option", { name, exact: true }).click();
  await expect(cancelButton(page, t)).toBeVisible();
}

/**
 * Opens the pill of a filter that already has a value, i.e. one the row shows. Its own text is label plus
 * value, so the pill is found by the name it carries for a screen reader (see [FilterPillShell]).
 */
export async function reopenPill(
  page: Page,
  t: Translate,
  name: string
): Promise<void> {
  await page
    .getByRole("button", { name: t("filter.editEntry", { arg0: name }) })
    .click();
  await expect(cancelButton(page, t)).toBeVisible();
}

export interface ListRequestBody {
  entries: {
    field: string;
    value: { from?: string; to?: string; periodKind?: string };
  }[];
}

/** The body of the next list request the page sends — where the filter actually shows up. */
export async function listRequest(
  page: Page,
  entity: string
): Promise<ListRequestBody> {
  const request = await page.waitForRequest(
    (candidate) =>
      candidate.url().includes(`/rs/${entity}/list`) &&
      candidate.method() === "POST",
    { timeout: 15_000 }
  );
  return JSON.parse(request.postData() ?? "{}") as ListRequestBody;
}

/** Throws the stored filter of a list away, so a case cannot leak into the next one. */
export async function resetFilter(page: Page, entity: string): Promise<void> {
  await page.request
    .get(`/rs/${entity}/filter/reset`, { headers: { "X-PF-Frontend": "next" } })
    .catch(() => undefined);
}
