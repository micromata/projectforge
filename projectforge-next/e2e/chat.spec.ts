import type { Page } from "@playwright/test";
import { test, expect, goto, loginWithOwnSession } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { userFormat } from "./fixtures/format";
import { isPluginActive } from "./fixtures/plugins";
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
 * a user outside the room is refused. The room is deleted afterwards (with its conversation). Direct messages
 * between the two, with a file in the personal box of the recipient; files of a room in its data transfer area;
 * a coloured text, inline code and a code block of several lines; a confidential message (end-to-end encrypted,
 * received once, in no message of the conversation).
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

  test("direct messages with a file in the personal box of the recipient", async ({
    loggedInPage: page,
    browser,
  }) => {
    test.skip(!hasRole("normalo-user"), "No normalo account on this instance.");
    test.skip(
      !(await isPluginActive(page.request, "datatransfer")),
      "Data transfer plugin not active."
    );
    const { t } = await userFormat(page);
    const other = await browser.newPage({
      baseURL: test.info().project.use.baseURL,
    });
    try {
      await loginWithOwnSession(other, "normalo-user");
      const otherName = (
        (await (await other.request.get("/rs/userStatus")).json()) as {
          userData: { fullname: string };
        }
      ).userData.fullname;

      // Started by the dialog on the chat page, choosing the other one.
      await goto(page, "/chat");
      await page.getByRole("button", { name: t("chat.direct.new") }).click();
      const dialog = page.getByRole("dialog");
      await dialog.getByRole("combobox").click();
      await page.keyboard.type("Normalo");
      await page
        .getByRole("option", { name: /Normalo/ })
        .first()
        .click();
      await dialog.getByRole("button", { name: t("chat.direct.open") }).click();
      await expect(page).toHaveURL(/\/chat\/\d+/);
      await expect(
        page.getByRole("heading", { name: otherName })
      ).toBeVisible();
      const conversationId = Number(page.url().match(/\/chat\/(\d+)/)![1]);

      // A text with a file: the file goes into the personal box of the other one.
      const text = `private ${uniqueSuffix()}`;
      await page.locator('input[type="file"]').setInputFiles({
        name: "e2e-offer.pdf",
        mimeType: "application/pdf",
        buffer: Buffer.from("%PDF-1.4 e2e"),
      });
      await expect(
        page.getByText(t("chat.file.directTarget", { arg0: otherName }))
      ).toBeVisible();
      const editor = page.getByRole("textbox", {
        name: t("chat.message.placeholder"),
      });
      await editor.click();
      await editor.pressSequentially(text);
      await editor.press("Enter");
      const sent = page.locator("article").filter({ hasText: text });
      await expect(
        sent.getByRole("link", { name: new RegExp("e2e-offer.pdf") })
      ).toBeVisible();

      // The other one: listed, the same conversation, the file downloadable, answers.
      const { t: otherT } = await userFormat(other);
      await goto(other, "/chat");
      await expect(
        other.locator(`a[href$="/chat/${conversationId}"]`)
      ).toBeVisible();
      await goto(other, `/chat/${conversationId}`);
      const received = other.locator("article").filter({ hasText: text });
      await expect(received).toBeVisible({ timeout: 10_000 });
      const href = await received
        .getByRole("link", { name: new RegExp("e2e-offer.pdf") })
        .getAttribute("href");
      const download = await other.request.get(href!.replace(/^\/next/, ""));
      expect(download.ok()).toBeTruthy();
      expect(download.headers()["content-disposition"]).toContain("attachment");
      const reply = other.getByRole("textbox", {
        name: otherT("chat.message.placeholder"),
      });
      await reply.click();
      await reply.pressSequentially(`Thanks, got it ${text}`);
      await reply.press("Enter");
      await expect(page.getByText(`Thanks, got it ${text}`)).toBeVisible({
        timeout: 10_000,
      });

      // Nobody else reads it.
      if (hasRole("finance-user")) {
        const outsider = await browser.newPage({
          baseURL: test.info().project.use.baseURL,
        });
        await loginWithOwnSession(outsider, "finance-user");
        const refused = await outsider.request.get(
          `/rs/chat/conversation/${conversationId}`
        );
        expect(refused.status()).toBe(403);
        await outsider.close();
      }
    } finally {
      await other.close();
    }
  });

  test("a confidential message, end-to-end encrypted, received once", async ({
    loggedInPage: page,
    browser,
  }) => {
    test.skip(!hasRole("normalo-user"), "No normalo account on this instance.");
    const { t } = await userFormat(page);
    const other = await browser.newPage({
      baseURL: test.info().project.use.baseURL,
    });
    try {
      await loginWithOwnSession(other, "normalo-user");
      const otherName = (
        (await (await other.request.get("/rs/userStatus")).json()) as {
          userData: { fullname: string };
        }
      ).userData.fullname;
      const opened = await page.request.post("/rs/chatDirect/open", {
        headers: await writeHeaders(page.request),
        data: { userId: await userId(other) },
      });
      expect(opened.ok()).toBeTruthy();
      const { conversationId } = (await opened.json()) as {
        conversationId: number;
      };
      const { t: otherT } = await userFormat(other);
      await goto(page, `/chat/${conversationId}`);
      await goto(other, `/chat/${conversationId}`);
      // The dev server's indicator covers the buttons of the composer (bottom left).
      for (const it of [page, other])
        await it.addStyleTag({
          content: "nextjs-portal { display: none !important; }",
        });

      // The sender offers, the recipient accepts: both see the same fingerprint.
      await page.getByRole("button", { name: t("chat.secret.action") }).click();
      const sendDialog = page.getByRole("dialog");
      const offered = page.waitForResponse((res) =>
        res.url().endsWith("/rs/chatSecret/offer")
      );
      await sendDialog
        .getByRole("button", { name: t("chat.secret.start") })
        .click();
      const sessionId = ((await (await offered).json()) as { id: string }).id;
      const offer = other.getByTestId("chat-secret-offer");
      await expect(offer).toBeVisible({ timeout: 10_000 });
      await offer
        .getByRole("button", { name: otherT("chat.secret.accept") })
        .click();
      const receiveDialog = other.getByRole("dialog");
      const fingerprint = sendDialog.getByTestId("chat-secret-fingerprint");
      await expect(fingerprint).toBeVisible({ timeout: 10_000 });
      const digits = await fingerprint.locator("span").textContent();
      expect(digits).toMatch(/^\d{5} \d{5} \d{5} \d{5}$/);
      await expect(
        receiveDialog.getByTestId("chat-secret-fingerprint").locator("span")
      ).toHaveText(digits!);

      // Encrypted and sent, shown once at the recipient.
      const secret = `pw-${uniqueSuffix()}`;
      await sendDialog
        .getByRole("textbox", { name: t("chat.secret.text") })
        .fill(secret);
      await sendDialog
        .getByRole("button", { name: t("chat.secret.send") })
        .click();
      await receiveDialog
        .getByRole("button", { name: otherT("chat.secret.show") })
        .click({ timeout: 10_000 });
      await expect(receiveDialog.getByTestId("chat-secret-text")).toHaveValue(
        secret
      );
      await expect(
        sendDialog.getByText(t("chat.secret.received", { arg0: otherName }))
      ).toBeVisible({ timeout: 10_000 });

      // Nothing left: not taken twice, not in the messages of the conversation.
      const again = await other.request.post(
        `/rs/chatSecret/${sessionId}/take`,
        { headers: await writeHeaders(other.request), data: {} }
      );
      expect(again.status()).toBe(403);
      const messages = await page.request.get(
        `/rs/chat/conversation/${conversationId}/messages`
      );
      expect(await messages.text()).not.toContain(secret);
      await other.keyboard.press("Escape");
      await expect(other.getByText(secret)).toHaveCount(0);
    } finally {
      await other.close();
    }
  });

  test("files of a room: an image as thumbnail, a document as download", async ({
    loggedInPage: page,
  }) => {
    test.skip(
      !(await isPluginActive(page.request, "datatransfer")),
      "Data transfer plugin not active."
    );
    const name = `ZZ e2e chat files ${uniqueSuffix()}`;
    const saved = await page.request.post("/rs/chatRoom/save", {
      headers: await writeHeaders(page.request),
      data: { name, users: [], fileExpiryDays: 7 },
    });
    expect(saved.ok()).toBeTruthy();
    const room = (await saved.json()) as { id: number; conversationId: number };
    try {
      const { t } = await userFormat(page);
      await goto(page, `/chat/${room.conversationId}`);
      await page.locator('input[type="file"]').setInputFiles([
        {
          name: "e2e-pixel.png",
          mimeType: "image/png",
          buffer: Buffer.from(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
            "base64"
          ),
        },
        {
          name: "e2e-notes.txt",
          mimeType: "text/plain",
          buffer: Buffer.from("notes"),
        },
      ]);
      // Files only, sent by Enter (the send button is covered by the dev overlay of next dev).
      await page
        .getByRole("textbox", { name: t("chat.message.placeholder") })
        .press("Enter");
      await expect(
        page.getByRole("button", { name: "e2e-pixel.png" })
      ).toBeVisible();
      await expect(
        page.getByRole("link", { name: new RegExp("e2e-notes.txt") })
      ).toBeVisible();
      // The expiry chosen is the one of the room.
      const stored = (await (
        await page.request.get(`/rs/chatRoom/${room.id}`)
      ).json()) as { fileExpiryDays: number };
      expect(stored.fileExpiryDays).toBe(7);
    } finally {
      await page.request.delete(`/rs/chatRoom/${room.id}`, {
        headers: await writeHeaders(page.request),
      });
    }
  });

  test("colours, inline code and a code block of several lines", async ({
    loggedInPage: page,
  }) => {
    const name = `ZZ e2e chat code ${uniqueSuffix()}`;
    const saved = await page.request.post("/rs/chatRoom/save", {
      headers: await writeHeaders(page.request),
      data: { name, users: [] },
    });
    expect(saved.ok()).toBeTruthy();
    const room = (await saved.json()) as { id: number; conversationId: number };
    try {
      const { t } = await userFormat(page);
      await goto(page, `/chat/${room.conversationId}`);
      const editor = page.getByRole("textbox", {
        name: t("chat.message.placeholder"),
      });
      const toolbar = page.getByRole("toolbar", {
        name: t("richTextEditor.toolbar"),
      });
      const marker = `colour ${uniqueSuffix()}`;
      await editor.click();
      await editor.pressSequentially(marker);
      await page.keyboard.press("ControlOrMeta+a");
      await toolbar
        .getByRole("button", { name: t("richTextEditor.color._") })
        .click();
      await page
        .getByRole("menuitem", { name: t("richTextEditor.color.red") })
        .click();
      await editor.press("End");
      // Inline code by its markdown, then a code block from the toolbar: Enter is a new line there.
      await editor.pressSequentially(" run `ls -la` now");
      await editor.press("Shift+Enter");
      await toolbar
        .getByRole("button", { name: t("richTextEditor.codeBlock") })
        .click();
      await editor.pressSequentially("fun a() {");
      await editor.press("Enter");
      await editor.pressSequentially("    b()");
      await editor.press("Enter");
      await editor.pressSequentially("}");
      await editor.press("ControlOrMeta+Enter");

      const sent = page.locator("article").filter({ hasText: marker });
      await expect(sent.locator('span[style*="color"]')).toContainText(marker);
      await expect(sent.locator("p code")).toHaveText("ls -la");
      // Exactly, with its lines and indentation (toHaveText would collapse the whitespace).
      await expect(sent.locator("pre code")).toHaveJSProperty(
        "textContent",
        "fun a() {\n    b()\n}"
      );
      await expect(editor).toHaveText("");

      // A code block by ``` and Enter, Tab indenting in it.
      const fenced = `fenced${uniqueSuffix()}`;
      await editor.pressSequentially("```");
      await editor.press("Enter");
      await editor.pressSequentially("if (x)");
      await editor.press("Enter");
      await editor.press("Tab");
      await editor.pressSequentially(fenced);
      await editor.press("ControlOrMeta+Enter");
      await expect(
        page.locator("article").filter({ hasText: fenced }).locator("pre code")
      ).toHaveJSProperty("textContent", `if (x)\n    ${fenced}`);
    } finally {
      await page.request.delete(`/rs/chatRoom/${room.id}`, {
        headers: await writeHeaders(page.request),
      });
    }
  });
});
