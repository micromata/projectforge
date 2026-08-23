import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { PAGE_SLOTS } from "../components/data-table/page-slots";

/**
 * The pagination's arrows stay where they are while the user pages through a list.
 *
 * Paging means clicking the same arrow again, so an arrow that moves has to be followed with the
 * mouse — which is what it did: the strip of page numbers between the two arrows grew a slot as the
 * current page left the first pages, and pushed them apart by some 50 px.
 *
 * Worth an e2e test because only a laid out page can show it: the fix is one of widths (see
 * page-slots.ts and `.pagination-slot`), and a unit test has no layout to measure.
 *
 * Reads only — no entry is created or changed.
 */
const ROW = "tbody tr[data-row-id]";

/** Sub-pixel rounding of the centred strip, not a jump the eye or the hand could follow. */
const TOLERANCE_PX = 2;

test("the paging arrows keep their position while paging", async ({
  loggedInPage: page,
}) => {
  const format = await userFormat(page);
  // The order book, because it is the list with enough entries to have pages to step through. Its
  // stored page size is the user's and is deliberately left alone.
  await goto(page, "/order");
  await page.locator(ROW).first().waitFor();

  const next = page.getByRole("button", {
    name: format.t("table.nextPage"),
    exact: true,
  });
  const slots = page.locator(".pagination-slot");
  // The last slot is the last page, which is how many there are.
  const pageCount = Number(await slots.last().innerText());
  // Fewer pages than slots means every page has one of its own and nothing can move in the first
  // place — there is then nothing for this test to see.
  test.skip(
    !(pageCount > PAGE_SLOTS),
    "this account's order book has too few pages to page through"
  );
  await expect(slots).toHaveCount(PAGE_SLOTS);

  const first = (await next.boundingBox())!.x;
  // Far enough for the window of page numbers to leave the first pages, which is where it used to
  // grow: page 5 is the first with a gap on both sides of it.
  for (let step = 0; step < 6; step++) {
    await next.click();
    await expect(page.locator(".pagination-slot")).toHaveCount(PAGE_SLOTS);
    const x = (await next.boundingBox())!.x;
    expect(Math.abs(x - first)).toBeLessThanOrEqual(TOLERANCE_PX);
  }
});
