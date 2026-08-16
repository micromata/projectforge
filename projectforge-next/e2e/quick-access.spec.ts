import type { Locator, Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import type { MenuData, MenuItem } from "../lib/rs/types";

/**
 * The quick access search of the top navigation, against the live backend.
 *
 * Its content is the menu the server sends (`/rs/menu`, access-filtered and translated per user), so
 * the entries a case searches for are read from that response rather than named here — which module
 * a test account may see is a matter of its rights, and a German title would only ever match a
 * German account.
 *
 * Read-only: entries are searched and one Next route is opened. The `wa/` and `react/` targets are
 * inspected, never followed — only Spring serves those, so the Next dev server would answer with
 * its 404.
 */
test.describe("quick access", () => {
  test("unfolds a focused field from the magnifier and folds back on Escape", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/book");

    // Closed it is the magnifier alone: no placeholder is on screen until it is clicked.
    await expect(searchField(page, format)).toHaveCount(0);

    const field = await focusSearch(page, format);
    // Focused right away, so the first keystroke is already part of the term.
    await expect(field).toBeFocused();
    await expect(field).toHaveValue("");

    await page.keyboard.press("Escape");
    await expect(results(page)).toHaveCount(0);
    await expect(searchField(page, format)).toHaveCount(0);
  });

  test("is reachable by the keyboard shortcut", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/book");

    // Pressed with nothing focused, i.e. the case the shortcut is for: reaching the search from
    // wherever the user happens to be, without aiming at the magnifier first.
    await pressShortcut(page);
    await expect(searchField(page, format)).toBeFocused();
  });

  test("narrows the entries to what is typed, best match first", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const entries = await menuEntries(page);
    // A term the menu itself provides: the first word of some entry's title, which then has to be
    // a hit at the very start of that title and cannot be beaten by anything.
    const entry = entries.find((e) => firstWord(e.title).length >= 4);
    if (!entry) {
      throw new Error("No menu entry with a word long enough to search for.");
    }
    const term = firstWord(entry.title);

    await goto(page, "/book");
    const field = await focusSearch(page, format);
    await field.fill(term);

    const options = results(page).getByRole("option");
    // Ranked, not merely filtered: a title beginning with the term stands above one that has it in
    // the middle, and cmdk's own scoring is switched off so this is the search's own order.
    await expect(options.first()).toHaveText(
      new RegExp(`^${escape(term)}`, "i")
    );
    // Everything left over matches, and every entry that matches is left — the count follows from
    // the same rule the search applies, plus the data-search row.
    await expect(options).toHaveCount(matching(entries, term).length + 1);
  });

  test("navigates into this app on Enter", async ({ loggedInPage: page }) => {
    const format = await userFormat(page);
    const entries = await menuEntries(page);
    // A `next/` entry, so the case can follow it: the search pushes a client-side route for those.
    const entry = entries.find((e) => e.url.startsWith("next/"));
    if (!entry) {
      throw new Error(
        "No menu entry served by this app. Does MenuItemDefId still hand out `next/...` urls?"
      );
    }

    await goto(page, "/");
    const field = await focusSearch(page, format);
    await field.fill(entry.title);
    await expect(results(page).getByRole("option").first()).toHaveText(
      entry.title
    );
    // Enter reaches the list although the focus stayed in the field: both halves share one cmdk
    // root, which is what lets the field drive the hits.
    await page.keyboard.press("Enter");

    await expect(results(page)).toHaveCount(0);
    // And the slot folded back to the magnifier, rather than staying open with a term that answers a
    // question already answered.
    await expect(searchField(page, format)).toHaveCount(0);
    const path = entry.url.slice("next".length);
    await expect(page).toHaveURL(new RegExp(`${escape(path)}$`));
  });

  test("offers the entry it just opened as a recent one", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const entries = await menuEntries(page);
    const entry = entries.find((e) => e.url.startsWith("next/"));
    if (!entry) {
      throw new Error("No menu entry served by this app.");
    }

    await goto(page, "/");
    const field = await focusSearch(page, format);
    await field.fill(entry.title);
    // The history is the backend's, shared by all three frontends: the entry is reported, and only
    // the next `/rs/menu` carries it back. Awaiting both beats reopening the palette and hoping.
    await Promise.all([
      page.waitForResponse(
        (res) => res.url().includes("/rs/menu/recent") && res.status() === 204
      ),
      page.waitForResponse((res) => res.url().endsWith("/rs/menu") && res.ok()),
      page.keyboard.press("Enter"),
    ]);
    await expect(results(page)).toHaveCount(0);

    // Reopened with an empty field, which is the only state the history is shown in: with a term the
    // ranking is the answer and the history would push a worse match above a better one.
    await focusSearch(page, format);
    await expect(
      recentGroup(page, format).getByRole("option", {
        name: entry.title,
        exact: true,
      })
    ).toBeVisible();
  });

  test("offers an entry opened from the main menu as a recent one", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    // From the main menu dropdown, not from the palette: that is the path a user takes, and the
    // history exists for exactly those clicks rather than for what was searched here before.
    const entry = (await menuEntries(page)).find(
      (e) => e.fromMainMenu && e.url.startsWith("next/")
    );
    if (!entry) {
      throw new Error("No main menu entry served by this app.");
    }

    await goto(page, "/");
    // The trigger is a Radix MenubarTrigger, i.e. a `menuitem` itself and not a `button`. Retried,
    // for the same reason focusSearch retries: a click before hydration lands on nothing.
    const trigger = page.getByRole("menuitem", {
      name: format.t("menu.main.title"),
    });
    // A `link`, not a `menuitem`: the MenubarItems render `asChild`, so the role is the anchor's.
    const target = page.getByRole("link", { name: entry.title, exact: true });
    await expect(async () => {
      await trigger.click();
      await expect(target.first()).toBeVisible({ timeout: 1000 });
    }).toPass({ timeout: 30_000 });
    await Promise.all([
      page.waitForResponse(
        (res) => res.url().includes("/rs/menu/recent") && res.status() === 204
      ),
      page.waitForResponse((res) => res.url().endsWith("/rs/menu") && res.ok()),
      target.first().click(),
    ]);

    await focusSearch(page, format);
    await expect(
      recentGroup(page, format).getByRole("option", {
        name: entry.title,
        exact: true,
      })
    ).toBeVisible();
  });

  test("offers a legacy entry as a way out of this app", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const entries = await menuEntries(page);
    const entry = entries.find((e) => e.url.startsWith("wa/"));
    if (!entry) {
      throw new Error("No menu entry served by Wicket.");
    }

    await goto(page, "/book");
    const field = await focusSearch(page, format);
    await field.fill(entry.title);

    // Only that the entry is reachable: selecting it is a full page load to Spring, which the dev
    // server on :3000 does not serve.
    await expect(
      results(page).getByRole("option", { name: entry.title, exact: true })
    ).toBeVisible();
  });

  test("offers the full-text data search for a term that is no menu entry", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const term = "zzqx";

    await goto(page, "/book");
    const field = await focusSearch(page, format);
    await field.fill(term);

    // The one row left, so a term without a menu hit is no dead end.
    const options = results(page).getByRole("option");
    await expect(options).toHaveCount(1);
    await expect(options).toHaveText(
      format.t("menu.quickAccess.searchAllData", { arg0: term })
    );

    // Where it leads is checked on the request rather than by arriving: the Wicket search page is
    // Spring's, and the term has to reach it as the parameter SearchPage reads.
    const [request] = await Promise.all([
      page.waitForRequest(/\/wa\/search/),
      options.click(),
    ]);
    expect(new URL(request.url()).searchParams.get("searchString")).toBe(term);
  });
});

/**
 * Clicks the magnifier and answers with the field that unfolds in its place, already focused.
 *
 * Retried rather than clicked once: the field replaces the button on React's `onClick`, and the
 * button is in the server-rendered markup already — a click before hydration lands on nothing, and
 * the test would then wait for a field no one is going to render.
 */
async function focusSearch(page: Page, format: UserFormat): Promise<Locator> {
  const field = searchField(page, format);
  await expect(async () => {
    await page.getByRole("button", { name: label(format) }).click();
    await expect(field).toBeVisible({ timeout: 1000 });
  }).toPass({ timeout: 30_000 });
  await expect(results(page)).toBeVisible();
  return field;
}

/** `CMD-K` on macOS, `CTRL-K` elsewhere — the same distinction the search itself makes. */
async function pressShortcut(page: Page) {
  await expect(async () => {
    await page.keyboard.press("ControlOrMeta+k");
    await expect(results(page)).toBeVisible({ timeout: 1000 });
  }).toPass({ timeout: 30_000 });
}

function searchField(page: Page, format: UserFormat): Locator {
  return page.getByPlaceholder(format.t("menu.quickAccess.placeholder"));
}

/** The accessible name of both halves — the collapsed magnifier and the unfolded field. */
function label(format: UserFormat): string {
  return format.t("menu.quickAccess._");
}

/** The "recently used" group of the open palette, shown only while the field is empty. */
function recentGroup(page: Page, format: UserFormat): Locator {
  return results(page).getByRole("group", {
    name: format.t("menu.quickAccess.recent"),
  });
}

/** The hits, which hang below the field as a popover rather than covering the page as a modal. */
function results(page: Page): Locator {
  return page.locator('[data-slot="popover-content"]');
}

/** The first word of a title, at the boundaries the ranking uses. */
function firstWord(title: string): string {
  return title.split(/[\s\-/(]/)[0];
}

/**
 * Every navigable leaf of the user's menu, deduplicated by url as the search does.
 *
 * The same flattening as `lib/menu-search.ts`, spelled out again on purpose: a test that imported
 * the implementation would compare it against itself.
 */
async function menuEntries(page: Page): Promise<Entry[]> {
  const res = await page.request.get("/rs/menu", {
    headers: { "X-PF-Frontend": "next" },
  });
  const menu = (await res.json()) as MenuData;
  const entries: Entry[] = [];
  const seen = new Set<string>();
  let fromMainMenu = false;
  const collect = (items: MenuItem[] | undefined, category: string) => {
    items?.forEach((item) => {
      if (item.subMenu?.length) return collect(item.subMenu, item.title);
      if (!item.url || item.type === "RESTCALL" || seen.has(item.url)) return;
      seen.add(item.url);
      entries.push({
        title: item.title,
        url: item.url,
        category,
        fromMainMenu,
      });
    });
  };
  fromMainMenu = true;
  collect(menu.mainMenu?.menuItems, "");
  fromMainMenu = false;
  collect(menu.favoritesMenu?.menuItems, "");
  // The account entries hang below a single item carrying the user's name, which is no category.
  menu.myAccountMenu?.menuItems.forEach((item) =>
    collect(item.subMenu ?? [item], "")
  );
  return entries;
}

interface Entry {
  title: string;
  url: string;
  /** Title of the entry's category, searchable in the palette as well — hence needed here. */
  category: string;
  /** Whether the main menu dropdown offers it, i.e. whether a case can click it there. */
  fromMainMenu: boolean;
}

/** The entries a term matches, by title or by category, as the search decides it. */
function matching(entries: Entry[], term: string): Entry[] {
  const needle = term.toLowerCase();
  return entries.filter(
    (entry) =>
      entry.title.toLowerCase().includes(needle) ||
      entry.category.toLowerCase().includes(needle)
  );
}

/** A menu title goes into a RegExp, and "Lizenzen / Hardware" is not a pattern. */
function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
