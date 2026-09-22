import type { Locator, Page } from "@playwright/test";
import type { PeriodKind } from "../../lib/date-period";
import type { UserFormat } from "./format";

/**
 * The quick access to a period — the art select between the two paging arrows, on a form as well as in a
 * list filter (see [PeriodStepper]).
 *
 * Shared because all of it is the same control everywhere, and because the arts must not be spelled out:
 * their names come from the account's catalog and the counted ones carry a number.
 */

/**
 * How a period art is named to this account — "3 Monate" is `duration.months` with its count filled in,
 * and the short form the trigger shows once it is picked is the same statement in two characters.
 */
export function kindName(
  format: UserFormat,
  kind: PeriodKind,
  short = false
): string {
  const key = short ? kind.shortLabelKey : kind.labelKey;
  const values: Record<string, string | number> =
    kind.labelArg == null ? {} : { arg0: kind.labelArg };
  // A label key can be both a text and a namespace in the bundle (`calendar.month` also parents the
  // month names), where the leaf is `<key>._` and the bare key throws `INSUFFICIENT_PATH` — as
  // `leafKeyOf` resolves for the app and `label()` does for a field. Mirror that here.
  try {
    return format.t(`${key}._`, values);
  } catch {
    return format.t(key, values);
  }
}

/** The art select; its accessible name is `duration.choose` on both surfaces. */
export function picker(page: Page, format: UserFormat): Locator {
  return page.getByRole("combobox", { name: format.t("duration.choose") });
}

/**
 * Picks an art. Picking the one already in effect is a move of its own — it sets the current period of
 * that art — so this is how a spec jumps there too.
 */
export async function pickKind(
  page: Page,
  format: UserFormat,
  kind: PeriodKind
): Promise<void> {
  await picker(page, format).click();
  await page
    .getByRole("option", { name: kindName(format, kind), exact: true })
    .click();
}

/**
 * Picks the "Eigener Zeitraum" entry, which releases the art in effect while keeping the two dates — the
 * first entry the picker grows once an art is on (see [PeriodQuickSelect]).
 */
export async function pickCustom(
  page: Page,
  format: UserFormat
): Promise<void> {
  await picker(page, format).click();
  await page
    .getByRole("option", { name: format.t("duration.custom"), exact: true })
    .click();
}
