import { expect } from "@playwright/test";
import { goto, test, waitForHydration } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The window itself never scrolls: only the columns inside the shell do (PageShell is
 * h-screen/overflow-hidden).
 *
 * What breaks it is not obvious from either end. Tailwind's `sr-only` is `position: absolute`, and the
 * scrolled column of an edit page carries such elements deep inside it — the file inputs behind the
 * attachment buttons (AttachmentAddButton, AttachmentDropArea, InvoicePdfField). Clipping follows the
 * containing block chain rather than the DOM parent chain, so unless a positioned ancestor catches
 * them their containing block is <body>: they escape the column, the *document* grows to the height of
 * the whole form, and the scroll left over at the end of the column chains into it. That lifts the app
 * out of the viewport — the action bar with it — and leaves white below it, which is how this was
 * reported.
 *
 * Hence the probe: a bare `sr-only` element pinned to the end of the column, standing for the ones the
 * pages put there. The two assertions are the same invariant from both sides, before and after — a
 * page whose own escapees happen to sit inside a `relative` parent would pass the second one on its
 * own and say nothing.
 *
 * Reads only — the seeded book is opened, nothing is saved.
 */
test("the window does not scroll, not even past the end of a column", async ({
  loggedInPage: page,
  seededBook,
}) => {
  // A short window, so the form overflows its column however few fields the entry has filled.
  await page.setViewportSize({ width: 1000, height: 500 });
  await goto(page, `/book/${seededBook.id}`);
  await waitForHydration(page);

  const documentScroll = () =>
    page.evaluate(() => ({
      overflow:
        document.documentElement.scrollHeight -
        document.documentElement.clientHeight,
      scrollY: window.scrollY,
    }));
  const column = page.locator("main div.overflow-y-auto").first();
  await expect
    .poll(() => column.evaluate((el) => el.scrollHeight - el.clientHeight))
    .toBeGreaterThan(100);

  await expect.poll(documentScroll).toEqual({ overflow: 0, scrollY: 0 });

  // An `sr-only` element at the end of the column, at the depth the real ones sit at.
  await column.evaluate((el) => {
    const probe = document.createElement("span");
    probe.className = "sr-only";
    probe.dataset.testid = "scroll-probe";
    el.lastElementChild?.append(probe);
  });
  expect(await documentScroll()).toEqual({ overflow: 0, scrollY: 0 });

  // Wheel well past the end of the column: what is left over is what would chain into the document.
  await page.mouse.move(500, 300);
  for (let i = 0; i < 20; i += 1) {
    await page.mouse.wheel(0, 400);
  }
  expect(await documentScroll()).toEqual({ overflow: 0, scrollY: 0 });

  // And the action bar is still where it belongs, at the bottom edge of the window.
  const box = await page
    .getByRole("button", { name: (await userFormat(page)).t("save") })
    .boundingBox();
  expect(box?.y).toBeGreaterThan(400);
});
