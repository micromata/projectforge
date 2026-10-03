import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The Excel export of the liquidity forecast tab, successor of the extra sheets of Wicket's liquidity
 * export (cash flow, all entries, debitor and creditor invoices). What the workbook holds is the
 * backend's business and covered by `LiquidityForecastExcelExportTest`; here it is the plumbing: the
 * button sits on the forecast tab and hands over a workbook.
 *
 * `@parallel`: the forecast is shown with the user's stored parameters, which the tab saves back
 * unchanged, and the export itself stores nothing.
 */
test.describe("liquidity forecast export", { tag: "@parallel" }, () => {
  test("exports the forecast as an Excel file", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/liquidity");
    await page
      .getByRole("tab", {
        name: format.t("plugins.liquidityplanning.forecast._"),
      })
      .click();

    const button = page.getByRole("button", { name: format.t("exportAsXls") });
    // Disabled until the forecast is loaded — the export is for the parameters the charts show.
    await expect(button).toBeEnabled();

    const download = page.waitForEvent("download");
    await button.click();
    // Named by the backend through Content-Disposition.
    expect((await download).suggestedFilename()).toMatch(/\.xlsx$/);
  });
});
