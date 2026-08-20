import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import { MARKER } from "./fixtures/seed";
import {
  createInvoice,
  fetchFormDefaults,
  removeInvoice,
} from "./fixtures/invoice";
import type { Page } from "@playwright/test";

/**
 * The documents of one invoice: the Word export beside the heading (InvoiceExportMenu) and the e-invoice
 * section of the form (EInvoiceSection).
 *
 * What the Word cases are about is the shape of the offer, not the document: whether it is a plain button or
 * a menu follows from `projectforge.invoiceTemplate` — an installation without a custom template has exactly
 * one, unnamed variant — so every case reads the variants from the backend and branches on them instead of
 * assuming a configuration.
 *
 * The invoice is created through the API and marked deleted afterwards; the one case that saves through the
 * form does so on such a throwaway invoice (see fixtures/invoice.ts for why it is created as GEPLANT).
 */

/** In the subject of every invoice these tests create, so a leftover is recognisable in the list. */
const SUBJECT = `${MARKER} invoice export (delete me)`;

// The export processes a Word template against a live backend, and the first navigation to a route
// additionally waits for the dev server to compile it.
test.describe.configure({ timeout: 120_000 });

test.describe("outgoing invoice Word export", () => {
  test("downloads the invoice as a .docx of the configured template", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const variants = await templateVariants(page);
    let id: number | null = null;
    try {
      id = await createInvoice(
        page,
        [{ number: 1, text: `${SUBJECT} 1`, menge: 2, einzelNetto: 1000 }],
        { subject: SUBJECT }
      );
      await goto(page, `/invoice/${id}`);
      await waitForForm(page, format);

      const download = page.waitForEvent("download");
      await exportButton(page, format).click();
      if (variants.length > 1) {
        // Several templates, so the button is a menu trigger and its entries are what downloads — one per
        // variant, named after it (see InvoiceExportMenu).
        await exportMenu(page)
          .getByRole("menuitem", { name: variantLabel(variants[0], format) })
          .click();
      }
      // Named by `InvoiceService.getInvoiceFilename` through Content-Disposition, so the assertion is on
      // the extension rather than on a name this side made up.
      expect((await download).suggestedFilename()).toMatch(/\.docx$/);
    } finally {
      await removeInvoice(page, id);
    }
  });

  test("offers one entry per configured template variant", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const variants = await templateVariants(page);
    test.skip(
      variants.length < 2,
      "This installation configured a single template variant, so there is no menu."
    );
    let id: number | null = null;
    try {
      id = await createInvoice(
        page,
        [{ number: 1, text: `${SUBJECT} 1`, menge: 1, einzelNetto: 100 }],
        { subject: SUBJECT }
      );
      await goto(page, `/invoice/${id}`);
      await waitForForm(page, format);

      await exportButton(page, format).click();
      // Every variant the backend named, and nothing else: an entry too many would export a template that
      // isn't there, and Wicket's menu is built from the same list. Scoped to the menu, since the navigation
      // of the page carries `menuitem`s of its own.
      const menu = exportMenu(page);
      await expect(menu.getByRole("menuitem")).toHaveCount(variants.length);
      for (const variant of variants) {
        await expect(
          menu.getByRole("menuitem", { name: variantLabel(variant, format) })
        ).toBeVisible();
      }
    } finally {
      await removeInvoice(page, id);
    }
  });

  test("refuses the export of an invoice that was never saved, and says why", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/invoice/new");
    // The title of a new entity is the trailing text of the breadcrumb, not a heading (see EditPageHeader).
    await expect(
      page.getByText(format.t("fibu.rechnung.title.add"), { exact: true })
    ).toBeVisible({ timeout: 60_000 });

    // Disabled rather than absent: the document is built from the stored invoice, and an entry that is
    // simply missing reads as "this installation has no export" (see InvoiceExportMenu).
    await expect(exportButton(page, format)).toBeDisabled();
  });
});

/**
 * The e-invoice of one invoice — a section of the form rather than a dialog behind a menu entry (see
 * EInvoiceSection).
 *
 * Deliberately **not** skipped on `eInvoiceConfigured`: whether the seller of this installation is
 * configured is exactly what must not decide whether the section is there. Its fields are what an unfinished
 * e-invoice is missing, so they have to be reachable while it cannot be built — an unconfigured seller is one
 * line of the checklist, not a hidden section. What is asserted is therefore the offer and the refusal, not a
 * downloaded document; the two exports are covered by `OutgoingInvoiceEInvoiceTest`, which can configure a
 * seller.
 */
test.describe("outgoing invoice e-invoice", () => {
  test("offers the section with its two buttons, whatever the invoice is missing", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    let id: number | null = null;
    try {
      id = await createInvoice(
        page,
        [{ number: 1, text: `${SUBJECT} 1`, menge: 1, einzelNetto: 100 }],
        { subject: SUBJECT }
      );
      await goto(page, `/invoice/${id}`);
      await waitForForm(page, format);

      // The fields the checklist is about, in the same card as the checklist — the reason this is a section.
      await expect(
        page.getByLabel(label(format, "fibu.konto.street"), { exact: true })
      ).toBeVisible();
      // Both buttons save first and export afterwards, so both stay pressable however incomplete the
      // invoice is — this one is planned, i.e. has no invoice number and cannot be exported at all.
      // Refusing them here would take away the save that is the way out of exactly that.
      await expect(
        page.getByRole("button", {
          name: t("fibu.rechnung.eInvoice.saveAndXRechnung"),
        })
      ).toBeEnabled();
      await expect(
        page.getByRole("button", {
          name: t("fibu.rechnung.eInvoice.saveAndZugferd"),
        })
      ).toBeEnabled();
    } finally {
      await removeInvoice(page, id);
    }
  });

  test("saves from the section, stays on the form, and says why it cannot export", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    let id: number | null = null;
    try {
      id = await createInvoice(
        page,
        [{ number: 1, text: `${SUBJECT} 1`, menge: 1, einzelNetto: 100 }],
        { subject: SUBJECT }
      );
      await goto(page, `/invoice/${id}`);
      await waitForForm(page, format);

      // Something the checklist asks for, entered where the checklist names it.
      const city = page.getByLabel(label(format, "fibu.konto.city"), {
        exact: true,
      });
      await city.fill("Kassel");
      await page
        .getByRole("button", {
          name: t("fibu.rechnung.eInvoice.saveAndXRechnung"),
        })
        .click();

      // The first half of the button, and the one thing the plain save does differently: the page stays, so
      // the next thing on the checklist can be corrected here (see
      // OutgoingInvoiceEntityRest.saveAndCheckEInvoice).
      await expect(
        page.getByText(t("message.successfullChanged"))
      ).toBeVisible();
      await expect(page).toHaveURL(new RegExp(`/invoice/${id}(\\?|$)`));
      await expect(city).toHaveValue("Kassel");

      // The second half, on a planned invoice: it has no invoice number, so instead of a download the
      // checklist above names what is missing. That the export is refused is the backend's answer, whatever
      // this installation configured — asserted through the checklist, not through an absent download.
      await expect(
        page.getByText(t("fibu.rechnung.eInvoice.validationErrors")).first()
      ).toBeVisible();
    } finally {
      await removeInvoice(page, id);
    }
  });
});

/** The variants of the Word template as the form itself reads them — the configuration, not a guess. */
async function templateVariants(page: Page): Promise<string[]> {
  const { templateVariants } = await fetchFormDefaults(page);
  return templateVariants ?? [];
}

/** The form has arrived once the subject of the invoice is in its field. */
async function waitForForm(page: Page, format: UserFormat) {
  await expect(
    page.getByLabel(label(format, "fibu.rechnung.betreff"), { exact: true })
  ).toHaveValue(SUBJECT, { timeout: 60_000 });
}

/** The opened dropdown of the export — the page's navigation has `menuitem`s of its own. */
function exportMenu(page: Page) {
  return page.getByRole("menu");
}

function exportButton(page: Page, format: UserFormat) {
  return page.getByRole("button", {
    name: label(format, "fibu.rechnung.exportInvoice"),
    // Exact, because the e-invoice section's "XRechnung exportieren" *contains* this label — a substring
    // match resolves to both buttons (see EInvoiceActions).
    exact: true,
  });
}

/** What one menu entry is called — the same rule the component applies (see InvoiceExportMenu). */
function variantLabel(variant: string, format: UserFormat): string {
  return variant
    ? variant.replace(/_/g, " ")
    : format.t("fibu.rechnung.exportInvoice.template.default");
}
