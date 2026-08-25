import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { userFormat } from "./fixtures/format";
import {
  importSubject,
  datevCsvFile,
  findImportedInvoiceIds,
  removeImportedInvoices,
  fetchCreditorInvoice,
  type ImportSubject,
} from "./fixtures/creditor-invoice-import";

/**
 * The incoming-invoice (Kreditor) DATEV CSV import against the live backend — the hand-built, layout-free
 * flow (`IncomingInvoiceImportRest` + components/shared/import), reached from the creditor-invoice list.
 *
 * One end-to-end pass: from the list's Import button to the drop step, upload a throwaway DATEV CSV,
 * reconcile it against the ledger, tick the parsed row and commit it — then confirm the background job
 * really created the invoice and delete it again. The CSV's creditor is unique per run (MARKER + suffix),
 * so the cleanup finds exactly this run's invoices and a leftover of an earlier one cannot leak in.
 *
 * Run as `finance-user`: the FIBU READWRITE right the import asks for, without the admin group. The
 * instance may have no such account — then the file skips rather than fails, per CLAUDE.md.
 */

const ROUTE = "/creditor-invoice";
const ROLE = "finance-user";

// More than the default: a live upload, a server-side reconcile and a background commit job, and the
// first navigation to each route additionally waits for the dev server to compile it.
test.describe.configure({ timeout: 120_000 });

test.describe("creditor invoice import", () => {
  test.skip(
    !hasRole(ROLE),
    `No ${ROLE} account on this instance — see e2e/fixtures/credentials.ts.`
  );

  test.beforeEach(async ({ page }) => {
    await login(page, "/next/", ROLE);
  });

  test("uploads a DATEV CSV, reconciles it and commits the new invoice", async ({
    page,
  }) => {
    const format = await userFormat(page);
    const subject: ImportSubject = importSubject();
    let createdIds: number[] = [];
    try {
      // From the list, the way a user reaches the import: the action bar's Import button.
      await goto(page, ROUTE);
      await page
        .getByRole("button", { name: format.t("import._"), exact: true })
        .click();
      await expect(page).toHaveURL(/\/creditor-invoice-import$/);
      await expect(
        page.getByRole("heading", {
          name: format.t("fibu.eingangsrechnung.import.title"),
        })
      ).toBeVisible({ timeout: 60_000 });

      // Drop the CSV onto the (sr-only) file input, and wait for the server's parse to land as the view.
      const uploaded = page.waitForResponse(
        (r) => r.url().includes("/rs/incomingInvoiceImport/upload") && r.ok()
      );
      await page
        .locator('input[type="file"]')
        .setInputFiles(datevCsvFile(subject));
      await uploaded;

      // The parsed row is shown with its creditor — proof the upload parsed and rendered the CSV.
      await expect(page.getByText(subject.kreditor)).toBeVisible();

      // Reconcile against the ledger: with no matching invoice the row turns NEW and becomes tickable.
      const reconciled = page.waitForResponse(
        (r) => r.url().includes("/rs/incomingInvoiceImport/reconcile") && r.ok()
      );
      await page
        .getByRole("button", {
          name: format.t("common.import.action.reconcile"),
        })
        .click();
      const view = (await (await reconciled).json()) as {
        info?: {
          numberOfNewEntries?: number;
          numberOfDeletedEntries?: number;
        };
      };

      // The safety gate. Reconcile syncs a period: it would mark every ledger invoice in the imported
      // rows' date window that the CSV omits as a deletion, and "Select all" below would commit those
      // too. The far-future date (see the fixture) is meant to leave that window empty; this asserts it
      // did, and refuses to commit if even one deletion was proposed — the test must never touch an
      // invoice it did not create. It expects exactly its own row to have turned NEW.
      expect(
        view.info?.numberOfDeletedEntries ?? 0,
        "reconcile proposed deleting existing ledger invoices — aborting before commit"
      ).toBe(0);
      expect(
        view.info?.numberOfNewEntries ?? 0,
        "the uploaded row did not reconcile to a NEW invoice"
      ).toBeGreaterThan(0);

      // Safe now: with no deletions proposed, "Select all" ticks only this run's NEW row(s).
      await page
        .getByRole("button", {
          name: format.t("common.import.action.selectAll"),
        })
        .click();

      // Commit the ticked row: the endpoint enqueues the import job and answers a job id, on which the
      // page navigates back to the list.
      const committed = page.waitForResponse(
        (r) => r.url().includes("/rs/incomingInvoiceImport/commit") && r.ok()
      );
      await page
        .getByRole("button", { name: format.t("common.import.action.commit") })
        .click();
      await committed;
      await expect(page).toHaveURL(new RegExp(`${ROUTE}$`));

      // The job runs asynchronously, so the invoice appears a moment later — poll for it, then read it
      // back: the creditor and reference round-trip from the CSV through the import into the ledger.
      createdIds = await findImportedInvoiceIds(page, subject, 1);
      expect(
        createdIds.length,
        "the import created no invoice"
      ).toBeGreaterThan(0);
      const stored = await fetchCreditorInvoice(page, createdIds[0]);
      expect(stored.kreditor).toBe(subject.kreditor);
      expect(stored.referenz).toBe(subject.referenz);
    } finally {
      await removeImportedInvoices(page, createdIds);
    }
  });
});
