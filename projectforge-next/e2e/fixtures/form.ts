import type { Locator } from "@playwright/test";

/**
 * Replaces what a number box holds, the way a user does it: click in, select all, type.
 *
 * Not `fill()`. Playwright focuses the box and *inserts* over its selection, and a number box
 * rewrites its own text on focus — it drops the group separators while it is being edited (see
 * `NumberBox`). That render lands between the focus and the insert and clears the selection, so the
 * typed digits are appended instead of replacing anything: "2.000,00" filled with "1500" becomes
 * "2000,001500". Nothing a human can trigger — a click focuses long before the selection is made —
 * but every `fill` on such a box runs into it.
 */
export async function typeNumber(box: Locator, text: string): Promise<void> {
  await box.click();
  await box.page().keyboard.press("ControlOrMeta+a");
  await box.pressSequentially(text);
}
