import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import { purgeTestAttachments } from "./fixtures/attachments";
import { MARKER } from "./fixtures/seed";
import { createInvoice, removeInvoice } from "./fixtures/invoice";

/**
 * The invoice PDF of an outgoing invoice — Wicket's `fibu.rechnung.invoicePdf` fieldset, migrated as a field
 * of the form (see InvoicePdfField).
 *
 * The one thing only a running system can show is what one upload turns into on that section: the file is
 * stored as a regular JCR attachment marked `__INVOICE_PDF__`, so it appears twice — as the value of the
 * field and as a row of the attachment list. Downloadable and deletable from both, as the Wicket-era page
 * had it, and a delete on either side has to reach the other one: the two are separate queries of one file
 * (see AttachmentSection). What the row does not offer is the rename, which would overwrite the marker.
 *
 * Writes, therefore — there is no way to see an upload without performing one — onto a throwaway invoice of
 * its own, which is marked deleted again afterwards. The file names carry the `pf-e2e-` prefix so
 * `purgeTestAttachments` recognises what a killed run left behind (see fixtures/attachments.ts).
 */

const SUBJECT = `${MARKER} invoice pdf (delete me)`;

/** A minimal but real PDF: what is asserted is the storage, not the content. */
const PDF = {
  mimeType: "application/pdf",
  buffer: Buffer.from("%PDF-1.4\n%%EOF\n"),
};

// A live backend plus the JCR, and the first navigation to a route waits for the dev server to compile it.
test.describe.configure({ timeout: 120_000 });

test.describe("outgoing invoice PDF", () => {
  let id: number | null = null;

  test.beforeEach(async ({ loggedInPage: page }) => {
    id = await createInvoice(
      page,
      [{ number: 1, text: `${SUBJECT} 1`, menge: 1, einzelNetto: 100 }],
      { subject: SUBJECT }
    );
  });

  test.afterEach(async ({ loggedInPage: page }) => {
    // The invoice goes, but the JCR node hangs off its id and a soft delete doesn't touch it — so the
    // files are removed first, and by name rather than by what the case believes it uploaded.
    if (id != null) await purgeTestAttachments(page, "outgoingInvoice", id);
    await removeInvoice(page, id);
    id = null;
  });

  test("offers the uploaded invoice PDF for download in its field and as a locked attachment row", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    const name = "pf-e2e-invoice.pdf";
    await goto(page, `/invoice/${id}`);
    await waitForEInvoiceSection(page, format);

    await page
      .getByLabel(`${t("file.upload.choose")}: ${pdfTitle(format)}`)
      .setInputFiles({ name, ...PDF });

    // The field shows what is stored, with the size the backend formatted — and the name is the download,
    // named after the field because the row below carries the same file under its own name.
    const fieldLink = page.getByRole("link", {
      name: `${t("download._")}: ${pdfTitle(format)}`,
    });
    await expect(fieldLink).toBeVisible();
    await expect(fieldLink).toContainText(new RegExp(escapeRegExp(name)));
    // Through the generic attachment route, which serves this file like any other of the node — there is
    // no download endpoint under `/rs/outgoingInvoice/invoicePdf`.
    const href = await fieldLink.getAttribute("href");
    expect(href).toContain(`/rs/attachments/download/outgoingInvoice/${id}`);
    expect(href).toContain("listId=attachments");

    // The attachment list carries it as well, exactly once — that row is what the old React page had,
    // with a download and a delete of its own (named after the file, while the field's are named after
    // the field).
    await expect(
      page.getByRole("link", { name: `${t("download._")}: ${name}` })
    ).toHaveCount(1);
    await expect(
      page.getByRole("button", { name: `${t("delete")}: ${name}` })
    ).toHaveCount(1);
    // Only the rename is withheld: that dialog edits the description, which for this file is the marker
    // making it the invoice PDF (see AttachmentSection).
    await expect(
      page.getByRole("button", { name: `${t("edit")}: ${name}` })
    ).toHaveCount(0);

    await page
      .getByRole("button", { name: `${t("delete")}: ${pdfTitle(format)}` })
      .click();
    // No confirmation, unlike an attachment row: the field's own state is the answer of the write, and
    // it says "none" again as soon as the delete came back. The row goes with it.
    await expect(page.getByText(new RegExp(escapeRegExp(name)))).toHaveCount(0);
  });

  test("deleting the invoice PDF in the attachment list empties its field as well", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    const name = "pf-e2e-invoice-row.pdf";
    await goto(page, `/invoice/${id}`);
    await waitForEInvoiceSection(page, format);

    await page
      .getByLabel(`${t("file.upload.choose")}: ${pdfTitle(format)}`)
      .setInputFiles({ name, ...PDF });
    await expect(
      page.getByRole("button", { name: `${t("delete")}: ${name}` })
    ).toBeVisible();

    // The row's delete, which asks first — an attachment is gone for good, the JCR keeps no history.
    // The confirmation is looked up inside the dialog, as book-attachments.spec.ts explains: the bare
    // label outside it would still match the row's own button while the dialog is mounting.
    await page.getByRole("button", { name: `${t("delete")}: ${name}` }).click();
    await page
      .getByRole("alertdialog")
      .getByRole("button", { name: t("delete"), exact: true })
      .click();

    // Both views of the one file are empty now: the list because it wrote its own cache, the field
    // because the list said so (`onChanged`) — without that it would still offer a download. Each says
    // "nothing found" in its own place, so the count is two: the field's empty state and the list's.
    await expect(page.getByText(new RegExp(escapeRegExp(name)))).toHaveCount(0);
    await expect(page.getByText(t("nothingFound"))).toHaveCount(2);
  });

  test("refuses a file that is not a PDF, in the backend's own words", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    await goto(page, `/invoice/${id}`);
    await waitForEInvoiceSection(page, format);

    await page
      .getByLabel(`${t("file.upload.choose")}: ${pdfTitle(format)}`)
      .setInputFiles({
        name: "pf-e2e-invoice.txt",
        mimeType: "text/plain",
        buffer: Buffer.from("not a pdf\n"),
      });

    // `FileCheck`'s text, shown as it arrives — Wicket drops such a file without a word.
    await expect(
      page.getByText(t("file.upload.error.unsupportedFormat", { arg0: "pdf" }))
    ).toBeVisible();
  });
});

/**
 * Waits until the form of *this* invoice is there, e-invoice section included — the section the PDF field
 * belongs to, since the ZUGFeRD export is the only reason the file exists (see EInvoiceSection).
 *
 * The sections are cards of one scroll column rather than tabs of their own, so nothing has to be opened —
 * but the section reads the stored state, and asserting on an empty field before that arrived would pass for
 * the wrong reason.
 */
async function waitForEInvoiceSection(page: Page, format: UserFormat) {
  await expect(
    page.getByLabel(label(format, "fibu.rechnung.betreff"), { exact: true })
  ).toHaveValue(SUBJECT, { timeout: 60_000 });
  await expect(
    page.getByText(format.t("fibu.rechnung.invoicePdf.hint"))
  ).toBeVisible();
}

/** The label of the field, as the component builds it (the key has a `hint` subkey). */
function pdfTitle(format: UserFormat): string {
  return label(format, "fibu.rechnung.invoicePdf");
}

/** A file name in a regex: the dot before the extension must not match any character. */
function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
