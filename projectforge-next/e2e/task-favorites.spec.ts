import type { APIRequestContext } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { MARKER, uniqueSuffix, writeHeaders } from "./fixtures/seed";
import type { TaskFavorite } from "../lib/rs/task";

/**
 * The bookmark menu of the task picker (TaskFavoritesMenu) against the live backend: overwriting a
 * favorite points it at the picked task (`POST /rs/task/favorites/update`), keeping its name.
 *
 * The favorite is the spec's own (named after the run) and deleted again; the task form it is used on
 * is never saved.
 */
test.describe("task favorites", { tag: "@parallel" }, () => {
  test("overwriting a favorite points it at the picked task", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    const name = `${MARKER} favorite ${uniqueSuffix()}`;
    // Pointing at the child; the child's edit page has its parent, the seeded task, picked.
    const created = (
      await favoritesCall(page.request, "create", {
        name,
        taskId: seededTask.child.id,
      })
    ).find((favorite) => favorite.name === name);
    expect(created?.taskId).toBe(seededTask.child.id);
    const id = created!.id;
    try {
      await goto(page, `/task/${seededTask.child.id}`);
      await expect(
        page.getByRole("textbox", { name: label(format, "task.title") })
      ).toHaveValue(seededTask.child.title);

      await page
        .getByRole("button", { name: format.t("task.favorites.tooltip") })
        .click();
      const entry = page.getByRole("button", { name, exact: true });
      // Not the picked task's favorite yet, so not marked as the current one.
      await expect(entry).not.toHaveAttribute("aria-current", "true");

      const row = page
        .getByRole("dialog")
        .locator("div")
        .filter({ has: entry })
        .last();
      const updated = page.waitForResponse((response) =>
        response.url().includes("/rs/task/favorites/update")
      );
      await row
        .getByRole("button", { name: format.t("favorites.saveModification") })
        .click();
      expect((await updated).ok()).toBe(true);

      // Now it points at the picked task, so it is the current one — and stored that way.
      await expect(entry).toHaveAttribute("aria-current", "true");
      const stored = (await favoritesCall(page.request, "list")).find(
        (favorite) => favorite.id === id
      );
      expect(stored?.name).toBe(name);
      expect(stored?.taskId).toBe(seededTask.id);
    } finally {
      await favoritesCall(page.request, "delete", { id });
    }
  });
});

async function favoritesCall(
  request: APIRequestContext,
  action: "list" | "create" | "delete",
  params: Record<string, string | number> = {}
): Promise<TaskFavorite[]> {
  const query = new URLSearchParams(
    Object.entries(params).map(([key, value]) => [key, String(value)])
  );
  const url = `/rs/task/favorites/${action}?${query}`;
  const res =
    action === "list"
      ? await request.get(url, { headers: { "X-PF-Frontend": "next" } })
      : await request.post(url, { headers: await writeHeaders(request) });
  expect(res.ok(), `${action}: HTTP ${res.status()}`).toBe(true);
  return (await res.json()) as TaskFavorite[];
}
