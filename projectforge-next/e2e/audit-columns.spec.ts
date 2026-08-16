import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { AUFTRAG_METADATA } from "../lib/metadata/auftrag.generated";
import { ORDER_PAGE } from "../components/features/order/order.page";
import { columnIdOf } from "../lib/page-def/define-page";
import { AUDIT_COLUMN_NAMES } from "../lib/page-def/audit-columns";

/**
 * The two timestamps every declared list offers — `created` and `lastUpdate` (see
 * lib/page-def/audit-columns.ts) — against the live backend, on the order book.
 *
 * What only a live run can settle: whether the row *carries* the values. A page's lean row fills the
 * columns of its own declaration (`BaseDTO.copyFrom4ListRow`), so a column appended generically shows
 * an empty cell for every row unless the DTO copies the timestamps too — which is a contract between
 * two modules that no typecheck spans. `Auftrag.copyFrom4ListRow` is the one that had to learn it.
 *
 * Read-only, and the stored grid state is reset first: whether a column starts hidden is only a
 * statement about a user who never touched it.
 */
test.describe("audit columns", () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    // Drops the stored filter *and* the grid state (AbstractEntityRest.resetListFilter), so the
    // visibility under test is the declared one rather than one a previous run left behind.
    await page.request
      .get("/rs/order/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("shows the declared lastUpdate and offers created hidden", async ({
    loggedInPage: page,
  }) => {
    const { t, timestamp } = await userFormat(page);
    await goto(page, "/order");
    await expect(
      page.getByRole("heading", { name: t(ORDER_PAGE.titleKey) })
    ).toBeVisible({ timeout: 60_000 });

    // The order book declares `lastUpdate` itself, so it is a column of the table from the start —
    // under the label AuftragDO carries for it ("modified"), not one this test spells out.
    const modified = t(AUFTRAG_METADATA.fields.lastUpdate.i18nKey!);
    await expect(
      page.getByRole("columnheader", {
        name: new RegExp(`^${modified}(\\s|$)`),
      })
    ).toHaveCount(1);

    // And it holds a timestamp, not an empty cell: the lean row has to copy it (see the file comment).
    // Matched by shape through the app's own formatter — the account's locale decides the layout.
    const shape = timestampPattern(timestamp);
    const cells = page.locator("tbody tr").first().locator("td");
    await expect(cells.filter({ hasText: shape }).first()).toBeVisible();

    // `created` is the generic half: appended although the declaration never names it, and hidden
    // until the user asks for it.
    expect(ORDER_PAGE.columns.map(columnIdOf)).not.toContain("created");
    const created = t(AUFTRAG_METADATA.fields.created.i18nKey!);
    await expect(
      page.getByRole("columnheader", { name: new RegExp(`^${created}(\\s|$)`) })
    ).toHaveCount(0);

    // Switching it on is what makes it a column — and it, too, has to arrive filled. Matched on the
    // whole label, not a substring of it: "Spalten" is a prefix of the panel's own
    // "Spalten zurücksetzen" button, so a substring match hits two elements once the panel is open.
    // (Not on the tooltip either: it is the app's own, so it names the trigger only while it shows.)
    const panel = page.getByRole("button", {
      name: t("columns._"),
      exact: true,
    });
    await panel.click();
    const checkbox = page.locator("#col-created");
    await expect(checkbox).toHaveAttribute("data-state", "unchecked");
    await checkbox.click();
    await page.keyboard.press("Escape");
    const header = page.getByRole("columnheader", {
      name: new RegExp(`^${created}(\\s|$)`),
    });
    await expect(header).toHaveCount(1);
    await expect(
      page.locator("tbody tr").first().locator("td").filter({ hasText: shape })
    ).not.toHaveCount(0);

    // The reset returns to the declared visibility rather than to "everything visible": the column the
    // page declares stays, the appended one goes back to hidden. Also leaves the account as found.
    await panel.click();
    await page.getByRole("button", { name: t("columns.reset") }).click();
    await page.keyboard.press("Escape");
    await expect(header).toHaveCount(0);
    await expect(
      page.getByRole("columnheader", {
        name: new RegExp(`^${modified}(\\s|$)`),
      })
    ).toHaveCount(1);
  });

  test("names both of them, in the order they read", () => {
    // Guards the pair itself: it is what the backend's `copyAuditFieldsFrom` fills, and what a row
    // type has to carry.
    expect([...AUDIT_COLUMN_NAMES]).toEqual(["created", "lastUpdate"]);
  });
});

/**
 * A timestamp as the table writes it, as a pattern: the digits of a known instant are replaced by
 * classes, so the assertion tests the *shape* the user's locale and time zone produce rather than any
 * one value (the rows under test are the account's own data).
 */
function timestampPattern(format: (value: unknown) => string): RegExp {
  const sample = format("2026-03-04T05:06:00Z");
  return new RegExp(
    sample.replace(/[.*+?^${}()|[\]\\]/g, "\\$&").replace(/\d/g, "\\d")
  );
}
