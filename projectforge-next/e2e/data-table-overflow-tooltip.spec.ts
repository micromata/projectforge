import { test, expect, goto } from "./fixtures/auth";

/**
 * The overflow tooltip of the data table against the live backend.
 *
 * Read-only, and deliberately on the order list: it is the widest list in the app, so several of its
 * columns are narrower than their content on any screen — which is the condition the tooltip exists
 * for and which a fixture could not produce as honestly.
 *
 * The truncated cell is found by measuring rather than named: which column overflows depends on the
 * viewport, the account's stored column widths and the data, and a hard-coded column would pass or
 * fail for reasons that have nothing to do with the tooltip.
 */
test.describe("data table overflow tooltip", () => {
  const TOOLTIP = "[data-slot=tooltip-content]";

  /** Cell indices of the body, split by whether their content is clipped. */
  async function measureCells(page: import("@playwright/test").Page) {
    return page.evaluate(() => {
      const clipped = (el: HTMLElement) =>
        // +1: sub-pixel layout makes scrollWidth exceed clientWidth on text that fits.
        el.scrollWidth > el.clientWidth + 1;
      const cells = Array.from(
        document.querySelectorAll("table tbody td")
      ) as HTMLElement[];
      const overflowing: { index: number; text: string }[] = [];
      const fitting: number[] = [];
      cells.forEach((td, index) => {
        const all = [
          td,
          ...Array.from(td.querySelectorAll("*")),
        ] as HTMLElement[];
        const hit = all.find((el) => clipped(el) && el.innerText.trim());
        // A cell with a native title of its own is left to that title (see useOverflowTooltip).
        if (hit && !td.querySelector("[title]")) {
          overflowing.push({ index, text: hit.innerText.trim() });
        } else if (!hit && td.innerText.trim()) {
          fitting.push(index);
        }
      });
      return { overflowing, fitting };
    });
  }

  test("reveals the full content of clipped cells and headers", async ({
    loggedInPage: page,
  }) => {
    await goto(page, "/order");
    await expect(page.locator("table tbody tr").first()).toBeVisible({
      timeout: 30_000,
    });
    // The skeleton rows are rows too, and their cells never overflow.
    await expect(page.locator("[data-slot=skeleton]")).toHaveCount(0, {
      timeout: 60_000,
    });

    const { overflowing, fitting } = await measureCells(page);
    expect(
      overflowing.length,
      "no clipped cell in the order list — has a column width or the viewport changed?"
    ).toBeGreaterThan(0);

    const cell = overflowing[0];
    await page.locator("table tbody td").nth(cell.index).hover();
    // The tooltip waits out a delay, so hovering the whole way across a table does not flash one
    // tooltip per column.
    await expect(page.locator(TOOLTIP)).toBeVisible({ timeout: 5_000 });
    // First line only: a cell may render several, and the assertion is that nothing is clipped away.
    await expect(page.locator(TOOLTIP)).toContainText(cell.text.split("\n")[0]);

    // A cell whose content fits gets none — the tooltip is for what the column hides, not a
    // second rendering of every value.
    if (fitting.length > 0) {
      await page.mouse.move(0, 0);
      await expect(page.locator(TOOLTIP)).toHaveCount(0);
      await page.locator("table tbody td").nth(fitting[0]).hover();
      await page.waitForTimeout(1_500);
      await expect(page.locator(TOOLTIP)).toHaveCount(0);
    }

    // Headers too: a narrow column shows an abbreviated label, and the full one is what says which
    // column it is.
    const header = await page.evaluate(() => {
      const ths = Array.from(document.querySelectorAll("table thead th"));
      for (const th of ths) {
        const label = th.querySelector<HTMLElement>("[data-overflow-text]");
        if (label && label.scrollWidth > label.clientWidth + 1) {
          return { index: ths.indexOf(th), text: label.innerText.trim() };
        }
      }
      return null;
    });
    if (header) {
      await page.mouse.move(0, 0);
      await page.locator("table thead th").nth(header.index).hover();
      await expect(page.locator(TOOLTIP)).toBeVisible({ timeout: 5_000 });
      await expect(page.locator(TOOLTIP)).toContainText(header.text);
    }
  });
});
