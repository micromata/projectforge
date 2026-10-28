import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { E_INVOICE_NUMBER, xRechnungFile } from "./fixtures/e-invoice";

/**
 * The e-invoice checker against the live backend (`EInvoiceCheckerRest`): a file is read back and shown, a
 * file that can't be checked is refused with the reason, and the last result survives a reload. Writes
 * nothing but the session's last result, which no other spec reads.
 *
 * Serial: the tests share the session, and with it the last result.
 */
test.describe.configure({ mode: "serial" });

test.describe("e-invoice checker", { tag: "@parallel" }, () => {
  test("shows what an uploaded XRechnung contains, also after a reload", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/eInvoiceChecker");
    await expect(
      page.getByRole("heading", { name: t("fibu.eInvoiceChecker.title") })
    ).toBeVisible();

    const uploaded = page.waitForResponse(
      (r) => r.url().includes("/rs/eInvoiceChecker/upload") && r.ok()
    );
    await page.locator('input[type="file"]').setInputFiles(xRechnungFile());
    await uploaded;

    const result = page.getByTestId("e-invoice-check-result");
    await expect(result).toContainText(E_INVOICE_NUMBER);
    await expect(result).toContainText("E2E Verkauf GmbH");
    await expect(result).toContainText("E2E Kundin GmbH");
    await expect(result).toContainText("Softwareentwicklung");
    await expect(result).toContainText("Projektmanagement");
    await expect(
      result.getByRole("heading", { name: t("fibu.eInvoiceChecker.seller") })
    ).toBeVisible();
    await expect(
      result.getByRole("heading", { name: t("fibu.eInvoiceChecker.lineItems") })
    ).toBeVisible();

    await page.reload();
    await expect(page.getByTestId("e-invoice-check-result")).toContainText(
      E_INVOICE_NUMBER
    );
  });

  test("refuses a file that is no e-invoice with the reason", async ({
    loggedInPage: page,
  }) => {
    await goto(page, "/eInvoiceChecker");
    const refused = page.waitForResponse(
      (r) =>
        r.url().includes("/rs/eInvoiceChecker/upload") && r.status() === 400
    );
    await page.locator('input[type="file"]').setInputFiles({
      name: "notes.txt",
      mimeType: "text/plain",
      buffer: Buffer.from("no e-invoice", "utf-8"),
    });
    await refused;
    await expect(page.locator('[data-tone="error"]')).toBeVisible();
  });
});
