import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import { purgeTestAttachments } from "./fixtures/attachments";
import { createBook, type SeededBook } from "./fixtures/seed";

/**
 * The viewer of attachments (AttachmentViewer), on the attachments of a book of its own — the component is the
 * shared one of every entity, the order book's included. Files carry the `pf-e2e-` prefix and are purged before
 * every case.
 */

const TEXT = "pf-e2e-preview.txt";
const PDF = "pf-e2e-preview.pdf";
const SVG = "pf-e2e-preview.svg";

const FILES = [
  {
    name: PDF,
    mimeType: "application/pdf",
    buffer: Buffer.from("%PDF-1.4\n%%EOF\n"),
  },
  {
    name: TEXT,
    mimeType: "text/plain",
    buffer: Buffer.from("ProjectForge preview <b>not bold</b>\n"),
  },
  {
    name: SVG,
    mimeType: "image/svg+xml",
    buffer: Buffer.from('<svg xmlns="http://www.w3.org/2000/svg"></svg>'),
  },
];

async function upload(page: Page, t: UserFormat["t"]) {
  for (const file of FILES) {
    await page.getByLabel(t("file.upload.choose")).setInputFiles(file);
    await expect(
      page.getByRole("link", {
        name: `${t("download._")}: ${file.name}`,
        exact: true,
      })
    ).toBeVisible();
  }
}

test.describe("attachment preview", { tag: "@parallel" }, () => {
  let book: SeededBook;

  test.beforeAll(async ({ seedRequest }) => {
    book = await createBook(seedRequest);
  });

  test.beforeEach(async ({ loggedInPage: page }) => {
    await purgeTestAttachments(page, "book", book.id);
  });

  test.afterEach(async ({ loggedInPage: page }) => {
    await purgeTestAttachments(page, "book", book.id);
  });

  test("shows texts and PDFs, pages through them and offers the download", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, `/book/${book.id}`);
    await upload(page, t);

    // An svg runs scripts when shown on the origin, so it is never offered (and the backend refuses it).
    await expect(
      page.getByRole("button", { name: `${t("preview")}: ${SVG}` })
    ).toHaveCount(0);

    await page
      .getByRole("button", { name: `${t("preview")}: ${TEXT}` })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("heading", { name: TEXT })).toBeVisible();
    // Text stays text: the markup of the file is shown, not rendered.
    await expect(
      dialog.getByText("ProjectForge preview <b>not bold</b>")
    ).toBeVisible();
    await expect(
      dialog.getByRole("link", { name: t("download._") })
    ).toHaveAttribute("href", /\/download\/book\//);

    // The other file that can be shown, in the order of the list (newest first or last, so page either way).
    const next = dialog.getByRole("button", {
      name: t("attachment.view.next"),
    });
    const target = (await next.isEnabled())
      ? next
      : dialog.getByRole("button", { name: t("attachment.view.previous") });
    await target.click();
    await expect(dialog.getByRole("heading", { name: PDF })).toBeVisible();
    // Headless Chromium has no PDF viewer (`pdfViewerEnabled` false, as on phones): then the viewer says so
    // instead of showing an empty frame.
    if (await page.evaluate(() => navigator.pdfViewerEnabled)) {
      await expect(dialog.locator("iframe")).toHaveAttribute(
        "src",
        /\/preview\/book\//
      );
    } else {
      await expect(
        dialog.getByText(t("attachment.view.pdfNotSupported"))
      ).toBeVisible();
    }
    await expect(
      dialog.getByRole("link", { name: t("attachment.view.newTab") })
    ).toHaveAttribute("target", "_blank");
  });

  test("the backend answers the pdf inline and refuses the svg", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, `/book/${book.id}`);
    await upload(page, t);
    const hrefOf = async (name: string) =>
      (await page
        .getByRole("link", { name: `${t("download._")}: ${name}`, exact: true })
        .getAttribute("href"))!.replace("/download/", "/preview/");

    const pdf = await page.request.get(await hrefOf(PDF));
    expect(pdf.status()).toBe(200);
    expect(pdf.headers()["content-type"]).toContain("application/pdf");
    expect(pdf.headers()["content-disposition"]).toMatch(/^inline/);
    // Safari shows a PDF with its plugin only if the policy of the PDF allows it, and downloads it otherwise.
    const csp = pdf.headers()["content-security-policy"] ?? "";
    expect(csp).toContain("object-src 'self'");
    expect(csp).not.toContain("object-src 'none'");

    const svg = await page.request.get(await hrefOf(SVG));
    expect(svg.status()).toBe(415);
  });
});
