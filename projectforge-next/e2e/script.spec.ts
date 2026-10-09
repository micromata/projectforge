import type { Page } from "@playwright/test";
import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { label, userFormat } from "./fixtures/format";
import { MARKER } from "./fixtures/seed";
import { SCRIPT_METADATA } from "../lib/metadata/script.generated";
import { SCRIPT_PAGE } from "../components/features/script/script.page";
import { MY_SCRIPT_PAGE } from "../components/features/script/myscript.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/** The headers a state changing call needs — the CSRF token is read per call, not cached. */
async function writeHeaders(page: Page): Promise<Record<string, string>> {
  const status = await page.request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  return {
    "X-PF-Frontend": "next",
    "X-PF-CSRF-Token": csrfToken,
    "Content-Type": "application/json",
  };
}

/**
 * The script pages (SCRIPT_PAGE, MY_SCRIPT_PAGE and the execution, ScriptExecuteView) against the live
 * backend. Nothing is stored: the execution runs ad-hoc code, which leaves no entity behind.
 */
test.describe("scripts", { tag: "@lane-script" }, () => {
  test("lists the scripts under the labels of ScriptDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    await goto(page, "/script");

    await expect(
      page.getByRole("heading", { name: t(SCRIPT_PAGE.titleKey) })
    ).toBeVisible();
    for (const column of SCRIPT_PAGE.columns) {
      const key = columnHeaderKeyOf(column, SCRIPT_METADATA);
      await expect(
        page.getByRole("columnheader", { name: label(format, key) }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("executes ad-hoc code and shows its result", async ({
    loggedInPage: page,
  }) => {
    // Compiling a Kotlin script takes a while, the first one of a server's life the longest.
    test.setTimeout(120_000);
    const { t } = await userFormat(page);
    await goto(page, "/script/execute");

    const editor = page.locator(".cm-content");
    await expect(editor).toBeVisible();
    await editor.click();
    await page.keyboard.press("ControlOrMeta+A");
    await page.keyboard.insertText(`"${MARKER} " + (6 * 7)`);
    await page.getByRole("button", { name: t("execute"), exact: true }).click();

    // First: the log may repeat the result.
    await expect(page.getByText(`${MARKER} 42`).first()).toBeVisible({
      timeout: 100_000,
    });
  });

  test.describe("without the finance rights", () => {
    test.skip(
      !hasRole("normalo-user"),
      "no normalo-user in this instance's testAccounts.txt"
    );

    test("lists the user's scripts, and executes no code of their own", async ({
      page,
    }) => {
      await login(page, "normalo-user", "/myscript");
      const { t } = await userFormat(page);
      await expect(
        page.getByRole("heading", { name: t(MY_SCRIPT_PAGE.titleKey) })
      ).toBeVisible();

      // The hole the legacy endpoint had: a script without id was executed as posted, so anybody could
      // run code of their own (MyScriptExecuteRest has no ad-hoc execution).
      const response = await page.request.post("/rs/myScriptExecute/execute", {
        headers: await writeHeaders(page),
        data: { id: null, type: "KOTLIN", script: `"${MARKER}"` },
      });
      expect(response.ok()).toBe(false);
    });
  });
});
