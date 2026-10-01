import type { Locator, Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The texts of a form can be selected with the mouse and copied: a field's label, the value of a
 * select and the value of an entity picker (a project's customer). Each of them used to refuse it —
 * the label and the picker's button were `select-none`, the select opened on pointerdown, before a
 * drag could begin (see components/ui/select.tsx, lib/text-selection.ts).
 *
 * A drag must select and leave the popup shut; a plain click must still open it. Reads only.
 */
test.describe("copyable texts", () => {
  test("a label and a select's value can be selected", async ({
    loggedInPage: page,
    seededBook,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, `/book/${seededBook.id}`);

    const status = page.getByRole("combobox", { name: /^status/i });
    await expect(status).toContainText(t("book.status.disposed"));

    const statusLabel = page.locator('[data-slot="field-label"]', {
      hasText: /^status/i,
    });
    await dragAcross(page, statusLabel.first());
    expect(await selectedText(page)).toMatch(/status/i);

    await dragAcross(page, status.locator('[data-slot="select-trigger-text"]'));
    expect(await selectedText(page)).toContain(t("book.status.disposed"));
    await expect(page.getByRole("listbox")).toHaveCount(0);

    // A click on the value is still a click on the select.
    await status.locator('[data-slot="select-trigger-text"]').click();
    await expect(page.getByRole("listbox")).toBeVisible();
    await page.keyboard.press("Escape");
  });

  test("an entity picker's value can be selected", async ({
    loggedInPage: page,
    seededProject,
  }) => {
    await goto(page, `/project/${seededProject.id}`);

    const name = seededProject.customer.name;
    const picker = page.getByRole("combobox").filter({ hasText: name });
    await expect(picker).toBeVisible({ timeout: 30_000 });

    await dragAcross(page, picker.getByText(name));
    expect(await selectedText(page)).toContain(name);
    await expect(page.locator('[data-slot="popover-content"]')).toHaveCount(0);

    // Skipped where the project's number locks its customer: a read-only picker opens on nothing.
    if ((await picker.getAttribute("aria-disabled")) === "true") return;
    await picker.getByText(name).click();
    await expect(page.locator('[data-slot="popover-content"]')).toBeVisible();
    await page.keyboard.press("Escape");
  });

  test("a section's title can be selected without folding it", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    // The task's gantt and finance cards are collapsible (see task-edit.spec.ts).
    await goto(page, `/task/${seededTask.id}`);

    const trigger = page.locator('[data-slot="collapsible-trigger"]').first();
    await expect(trigger).toBeVisible({ timeout: 30_000 });
    const state = await trigger.getAttribute("data-state");
    // The title's own span (see SectionHeader): the trigger's text is shown uppercase.
    const title = trigger.locator("span.uppercase").first();
    const text = (await title.textContent())!.trim();

    await dragAcross(page, title);
    // As shown, i.e. in capitals: that is what the browser copies.
    expect((await selectedText(page)).toLowerCase()).toContain(
      text.toLowerCase()
    );
    await expect(trigger).toHaveAttribute("data-state", state!);

    // A click on the title still folds the section.
    await title.click();
    await expect(trigger).not.toHaveAttribute("data-state", state!);
  });
});

/** Presses the mouse at the start of `target`'s text and releases it at its end. */
async function dragAcross(page: Page, target: Locator) {
  await page.evaluate(() => window.getSelection()?.removeAllRanges());
  await target.scrollIntoViewIfNeeded();
  const box = await target.boundingBox();
  if (!box) throw new Error("target not visible");
  const y = box.y + box.height / 2;
  await page.mouse.move(box.x + 1, y);
  await page.mouse.down();
  await page.mouse.move(box.x + box.width - 1, y, { steps: 10 });
  await page.mouse.up();
}

function selectedText(page: Page): Promise<string> {
  return page.evaluate(() => window.getSelection()?.toString() ?? "");
}
