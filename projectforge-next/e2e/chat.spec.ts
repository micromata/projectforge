import type { Page } from "@playwright/test";
import { test, expect, goto, loginWithOwnSession } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { userFormat } from "./fixtures/format";
import { uniqueSuffix, writeHeaders } from "./fixtures/seed";

/** The id of the user logged in on the page. */
async function userId(page: Page): Promise<number> {
  const status = await page.request.get("/rs/userStatus");
  return ((await status.json()) as { userData: { userId: number } }).userData
    .userId;
}

/**
 * The chat (PF-CHAT) with a room of the default test user and the normalo user as participant, in two browsers:
 * a message with a mention chosen from the suggestions appears live at the other one, who replies in the thread;
 * a user outside the room is refused. The room is deleted afterwards (with its conversation).
 */
test.describe("chat", { tag: "@parallel" }, () => {
  test("mentions, live updates and threads between two participants", async ({
    loggedInPage: page,
    browser,
  }) => {
    test.skip(!hasRole("normalo-user"), "No normalo account on this instance.");
    const { t } = await userFormat(page);
    const other = await browser.newPage({
      baseURL: test.info().project.use.baseURL,
    });
    await loginWithOwnSession(other, "normalo-user");
    const otherId = await userId(other);
    const name = `ZZ e2e chat ${uniqueSuffix()}`;
    const saved = await page.request.post("/rs/chatRoom/save", {
      headers: await writeHeaders(page.request),
      data: { name, users: [{ id: otherId, displayName: "" }] },
    });
    expect(saved.ok()).toBeTruthy();
    const room = (await saved.json()) as { id: number; conversationId: number };
    try {
      await goto(page, `/chat/${room.conversationId}`);
      await expect(page.getByRole("heading", { name })).toBeVisible();
      await goto(other, `/chat/${room.conversationId}`);
      const { t: otherT } = await userFormat(other);
      await expect(
        other.getByText(otherT("chat.conversation.empty"))
      ).toBeVisible();

      // A mention chosen from the suggestions, sent by Enter.
      const editor = page.getByRole("textbox", {
        name: t("chat.message.placeholder"),
      });
      await editor.click();
      await editor.pressSequentially("Hello @Norm");
      await page
        .getByRole("option")
        .getByRole("button", { name: /Normalo/ })
        .waitFor();
      await editor.press("Enter");
      await editor.pressSequentially(" welcome");
      await editor.press("Enter");
      const sent = page.locator("article").filter({ hasText: "welcome" });
      await expect(sent.locator('[data-type="mention"]')).toContainText(
        "Normalo"
      );

      // Live at the other participant, marked as mentioning them.
      const received = other.locator("article").filter({ hasText: "welcome" });
      await expect(received).toBeVisible({ timeout: 10_000 });
      await received.hover();
      await received
        .getByRole("button", { name: otherT("chat.message.reply") })
        .click();
      const reply = other.getByRole("textbox", {
        name: otherT("chat.thread.placeholder"),
      });
      await reply.pressSequentially("Thanks!");
      await reply.press("Enter");
      await expect(
        other.getByRole("dialog").getByText("Thanks!")
      ).toBeVisible();

      // The thread summary at the author, live.
      await expect(
        page.getByRole("button", { name: new RegExp(t("chat.thread.reply")) })
      ).toBeVisible({
        timeout: 10_000,
      });

      // Somebody outside the room is refused (finance user, if there is one).
      if (hasRole("finance-user")) {
        const outsider = await browser.newPage({
          baseURL: test.info().project.use.baseURL,
        });
        await loginWithOwnSession(outsider, "finance-user");
        const refused = await outsider.request.get(
          `/rs/chat/conversation/${room.conversationId}`
        );
        expect(refused.status()).toBe(403);
        await outsider.close();
      }
    } finally {
      await page.request.delete(`/rs/chatRoom/${room.id}`, {
        headers: await writeHeaders(page.request),
      });
      await other.close();
    }
  });
});
