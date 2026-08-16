import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { ORDER_PAGE } from "../components/features/order/order.page";

/**
 * The overflow tooltip of the data table against the live backend.
 *
 * On the order list, filtered down to the order the tests created: `seededOrder` carries a title far
 * wider than its column, so a clipped cell is guaranteed rather than hoped for — on a fresh database
 * the list would otherwise be empty, and on this one the clipped cells would be production content.
 *
 * The truncated cell is still found by measuring rather than named: which column overflows depends on
 * the viewport and the account's stored column widths, and a hard-coded column would pass or fail for
 * reasons that have nothing to do with the tooltip.
 */
test.describe("data table overflow tooltip", () => {
  const TOOLTIP = "[data-slot=tooltip-content]";

  /**
   * Cell indices of one row, split by whether their content is clipped.
   *
   * Indices within that row, so hovering can address them again: the row itself is located by the
   * text the caller passes, because a search may leave more rows than the one asked for (the backend
   * matches a term anywhere) and the guaranteed-wide title is in exactly one of them.
   */
  async function measureCells(
    page: import("@playwright/test").Page,
    rowText: string
  ) {
    return page.evaluate((text) => {
      // The same measurement the hook makes, and deliberately not `scrollWidth`: that one is a
      // rounded-up integer over the whole scrollable area and exceeds `clientWidth` on cells whose
      // text fits, so a spec built on it would expect a tooltip where the user needs none.
      const clipped = (el: HTMLElement) => {
        const style = getComputedStyle(el);
        if (style.overflowX !== "hidden" && style.overflowX !== "clip") {
          return false;
        }
        const available =
          el.clientWidth -
          parseFloat(style.paddingLeft) -
          parseFloat(style.paddingRight);
        const range = document.createRange();
        range.selectNodeContents(el);
        return range.getBoundingClientRect().width > available + 1;
      };
      const row = Array.from(document.querySelectorAll("table tbody tr")).find(
        (tr) => (tr as HTMLElement).innerText.includes(text)
      );
      if (!row) return { overflowing: [], fitting: [] };
      const cells = Array.from(row.querySelectorAll("td")) as HTMLElement[];
      const overflowing: { index: number; text: string }[] = [];
      const fitting: number[] = [];
      cells.forEach((td, index) => {
        // Innermost first, like the hook: a cell renderer's inner span is what clips the text, and a
        // range over the cell would measure that span's box rather than the text in it.
        const all = [
          ...Array.from(td.querySelectorAll("*")).reverse(),
          td,
        ] as HTMLElement[];
        const hit = all.find((el) => clipped(el) && el.innerText.trim());
        // A declared tooltip wins over the clipped text (see useOverflowTooltip), so such a cell says
        // nothing about the overflow behaviour.
        if (hit && !td.querySelector("[data-tooltip]")) {
          overflowing.push({ index, text: hit.innerText.trim() });
        } else if (!hit && td.innerText.trim()) {
          fitting.push(index);
        }
      });
      return { overflowing, fitting };
    }, rowText);
  }

  test("reveals the full content of clipped cells and headers", async ({
    loggedInPage: page,
    seededOrder,
  }) => {
    await goto(page, "/order");
    await expect(page.locator("table tbody tr").first()).toBeVisible({
      timeout: 30_000,
    });
    // Narrowed to the seeded order, so the measured row is its own: its title is the one value that is
    // certainly wider than its column. The run's suffix as the term — the rest of the title is the
    // same in every run.
    const term = seededOrder.title.split(" ")[3] ?? seededOrder.title;
    await page
      .getByPlaceholder(
        (await userFormat(page)).t(ORDER_PAGE.searchPlaceholderKey)
      )
      .fill(term);
    const row = page
      .locator("table tbody tr")
      .filter({ hasText: term })
      .first();
    await expect(row).toBeVisible({ timeout: 30_000 });
    // The skeleton rows are rows too, and their cells never overflow.
    await expect(page.locator("[data-slot=skeleton]")).toHaveCount(0, {
      timeout: 60_000,
    });

    const { overflowing, fitting } = await measureCells(page, term);
    expect(
      overflowing.length,
      "the seeded order's title must be too wide for its column"
    ).toBeGreaterThan(0);

    const cell = overflowing[0];
    await row.locator("td").nth(cell.index).hover();
    // The tooltip waits out a delay, so hovering the whole way across a table does not flash one
    // tooltip per column.
    await expect(page.locator(TOOLTIP)).toBeVisible({ timeout: 5_000 });
    // First line only: a cell may render several, and the assertion is that nothing is clipped away.
    await expect(page.locator(TOOLTIP)).toContainText(cell.text.split("\n")[0]);

    // A cell whose content fits gets none — the tooltip is for what the column hides, not a
    // second rendering of every value. Several of them rather than one: a tooltip repeating the
    // cell verbatim is what a too-lenient overflow measurement produces, and it shows up on the
    // ordinary short values (a date, an amount), not on the one cell a spec happens to pick.
    for (const index of fitting.slice(0, 4)) {
      await page.mouse.move(0, 0);
      await expect(page.locator(TOOLTIP)).toHaveCount(0);
      await row.locator("td").nth(index).hover();
      await page.waitForTimeout(1_500);
      await expect(
        page.locator(TOOLTIP),
        `the cell in column ${index} shows all of its text, so it needs no tooltip`
      ).toHaveCount(0);
    }

    // Headers too: a narrow column shows an abbreviated label, and the full one is what says which
    // column it is. Measured like the cells above, and only within the viewport — a column scrolled
    // out horizontally cannot be hovered.
    const header = await page.evaluate(() => {
      const ths = Array.from(document.querySelectorAll("table thead th"));
      for (const th of ths) {
        const label = th.querySelector<HTMLElement>("[data-overflow-text]");
        if (!label) continue;
        const style = getComputedStyle(label);
        const available =
          label.clientWidth -
          parseFloat(style.paddingLeft) -
          parseFloat(style.paddingRight);
        const range = document.createRange();
        range.selectNodeContents(label);
        const rect = label.getBoundingClientRect();
        if (
          range.getBoundingClientRect().width > available + 1 &&
          rect.right <= window.innerWidth
        ) {
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

    // A declared tooltip goes through the same single tooltip — the app's styled one, not the
    // browser's grey box. The order list always has one: the sort indicator of the sorted column.
    const declared = page.locator("table [data-tooltip]").first();
    const declaredText = await declared.getAttribute("data-tooltip");
    expect(
      declaredText,
      "the sorted column's indicator declares a tooltip"
    ).toBeTruthy();
    await page.mouse.move(0, 0);
    await expect(page.locator(TOOLTIP)).toHaveCount(0);
    await declared.hover();
    await expect(page.locator(TOOLTIP)).toBeVisible({ timeout: 5_000 });
    await expect(page.locator(TOOLTIP)).toContainText(declaredText!);
  });
});
