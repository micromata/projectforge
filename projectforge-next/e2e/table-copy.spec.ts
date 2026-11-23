import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { waitForRows } from "./fixtures/list-table";

// Opens the user list (its filter is stored per user, hence the lane); nothing is written.
test.describe("copy table", { tag: "@lane-user" }, () => {
  test("puts the header row and the rows of the list on the clipboard as tab separated text", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await page
      .context()
      .grantPermissions(["clipboard-read", "clipboard-write"]);
    await goto(page, "/user");
    const rows = await waitForRows(page);
    const shown = await rows.count();

    await page.getByRole("button", { name: t("export"), exact: true }).click();
    await page.getByRole("menuitem", { name: t("table.copy") }).click();
    await expect(page.getByText(t("table.copy.failed"))).toHaveCount(0);

    const text = await page.evaluate(() => navigator.clipboard.readText());
    const lines = text.split("\r\n");
    expect(lines.length).toBeGreaterThan(1);
    // Header plus at least the rows on screen (the list may be paged on the client).
    expect(lines.length - 1).toBeGreaterThanOrEqual(shown);
    expect(lines[0].split("\t").length).toBeGreaterThan(1);
  });
});
