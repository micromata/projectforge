import type { APIRequestContext, Page } from "@playwright/test";
import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole, readCredentials } from "./fixtures/credentials";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import { waitForRow } from "./fixtures/list-table";
import { USER_PAGE } from "../components/features/user/user.page";
import { PF_USER_METADATA } from "../lib/metadata/pf-user.generated";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The user page of projectforge-next — list and form, both rendered from USER_PAGE.
 *
 * Everything it asserts on is the E2E account of the finance role (see fixtures/credentials.ts), which
 * has groups and rights: the list of this database may be a production copy, so no user of it may be
 * named in the source. The test accounts exist on every development instance (`E2ETestAccountsService`).
 *
 * The one writing case changes the account's description and puts the stored value back afterwards.
 * Nothing here creates a user of its own: `PFUserDO` is historizable and can't be removed again.
 */
test.describe("user page", { tag: "@parallel" }, () => {
  test.skip(
    !hasRole("finance-user"),
    "no finance-user in this instance's testAccounts.txt"
  );
  const username = hasRole("finance-user")
    ? readCredentials("finance-user").username
    : "";

  test.beforeEach(async ({ loggedInPage: page }) => {
    // The filter is stored per user and per entity: a status left behind would decide which users show.
    await page.request
      .get("/rs/user/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("shows the declared columns under the labels of PFUserDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/user");
    await expect(
      page.getByRole("heading", { name: format.t("user.title.list") })
    ).toBeVisible({ timeout: 30_000 });

    for (const column of USER_PAGE.columns) {
      // Admin-only and LDAP columns are the backend's decision (`ColumnBase.visible`), hidden ones the
      // user's.
      if ("visible" in column || "hiddenByDefault" in column) continue;
      const key = columnHeaderKeyOf(column, PF_USER_METADATA);
      await expect(
        // By the header's own text: "Name" is also part of "Username" and "First name".
        page.getByRole("columnheader").filter({
          has: page.getByText(label(format, key), { exact: true }),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("narrows the list to the searched user and opens the form", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const id = await userIdOf(page.request, username);
    await goto(page, "/user");
    await page.getByPlaceholder(format.t("filter.searchList")).fill(username);
    const row = await waitForRow(page, username, 30_000);

    await row.click();

    await expect(page).toHaveURL(new RegExp(`/user/${id}$`));
    await expect(usernameField(page, format)).toHaveValue(username);
  });

  test("shows the rights the backend offers for the groups", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const id = await userIdOf(page.request, username);
    const rights = page.waitForResponse(
      (response) =>
        response.url().includes("/rs/user/rights") &&
        response.request().method() === "POST" &&
        (response.request().postData() ?? "").includes(`"userId":${id}`)
    );
    await goto(page, `/user/${id}`);
    const answer = (await (await rights).json()) as { label: string }[];

    await expect(usernameField(page, format)).toHaveValue(username, {
      timeout: 30_000,
    });
    if (answer.length === 0) {
      await expect(page.getByText(format.t("user.rights.none"))).toBeVisible();
    }
    for (const right of answer) {
      await expect(
        page.getByText(right.label, { exact: true }).first()
      ).toBeVisible();
    }
    // Loading the rights leaves the form as it was loaded: nothing to save yet.
    await expect(
      page.getByRole("button", { name: format.t("save"), exact: true })
    ).toBeDisabled();
  });

  test("changes the password in a dialog over the form", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const id = await userIdOf(page.request, username);
    let userSaved = false;
    page.on("request", (request) => {
      if (request.url().includes("/rs/user/saveorupdate")) userSaved = true;
    });
    await goto(page, `/user/${id}`);
    await expect(usernameField(page, format)).toHaveValue(username, {
      timeout: 30_000,
    });

    await page
      .getByRole("button", {
        name: format.t("menu.changePassword"),
        exact: true,
      })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog).toBeVisible();
    // Two different passwords: the backend refuses them, so the account keeps its password.
    await dialog
      .getByLabel(format.t("user.changePassword.newPassword"), { exact: true })
      .fill("pf-E2e-first-9731!");
    await dialog
      .getByLabel(format.t("passwordRepeat"), { exact: true })
      .fill("pf-E2e-second-9731!");
    await dialog
      .getByRole("button", { name: format.t("update"), exact: true })
      .click();

    // The refusal shows in the dialog, which stays open over the unchanged form.
    await expect(dialog.locator("[aria-invalid=true]").first()).toBeVisible();
    await expect(page).toHaveURL(new RegExp(`/user/${id}$`));
    await dialog
      .getByRole("button", { name: format.t("cancel"), exact: true })
      .click();
    await expect(dialog).toHaveCount(0);
    expect(userSaved).toBe(false);
  });

  test("saves a change and returns to the list", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const id = await userIdOf(page.request, username);
    const stored = await storedUser(page.request, id);
    const changed = `${stored.description ?? ""} [pf-e2e]`.trim();
    await goto(page, `/user/${id}`);
    const description = page.getByLabel(label(format, "description"), {
      exact: true,
    });
    await expect(description).toHaveValue(stored.description ?? "", {
      timeout: 30_000,
    });

    try {
      await description.fill(changed);
      await page
        .getByRole("button", { name: format.t("save"), exact: true })
        .click();

      await expect(
        page.getByText(format.t("message.successfullChanged"))
      ).toBeVisible();
      await expect(page).toHaveURL(/\/user$/);
      const after = await storedUser(page.request, id);
      expect(after.description).toBe(changed);
      expect(after.username).toBe(stored.username);
      // Groups and rights survive a save that didn't touch them.
      expect(after.assignedGroups?.map((group) => group.id)).toEqual(
        stored.assignedGroups?.map((group) => group.id)
      );
      expect(after.userRights).toEqual(stored.userRights);
      // Nor do the values the form doesn't carry (UserEntityRest.transformForDB).
      expect(after.lastLogin).toBe(stored.lastLogin);
    } finally {
      await restoreDescription(page.request, id, stored.description);
    }
  });

  test.afterAll(async ({ seedRequest }) => {
    await seedRequest
      .get("/rs/user/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });
});

/**
 * The same list for a user who may read it but not change it: `UserDao.hasAccess` is the admin group's,
 * so `listMeta` answers no insert and no update (`UserEntityRest.listUpdateAccess`).
 */
test.describe("user list without the admin right", { tag: "@parallel" }, () => {
  test.skip(
    !hasRole("normalo-user"),
    "no normalo-user in this instance's testAccounts.txt"
  );

  test("leaves the add button and the administrator's columns out", async ({
    page,
  }) => {
    await login(page, "normalo-user");
    const format = await userFormat(page);
    await goto(page, "/user");
    await expect(
      page.getByRole("heading", { name: format.t("user.title.list") })
    ).toBeVisible({ timeout: 30_000 });
    await expect(
      page.getByRole("link", { name: format.t("menu.addNewEntry") })
    ).toHaveCount(0);

    // An administrator's columns aren't even offered: not the audit pair every other list appends,
    // nor the LDAP values (development mode counts posix accounts as configured for everyone).
    await page
      .getByRole("button", { name: format.t("columns._"), exact: true })
      .click();
    await expect(page.locator("#col-username")).toHaveCount(1);
    for (const id of ["created", "lastUpdate", "ldapValues", "description"]) {
      await expect(page.locator(`#col-${id}`), id).toHaveCount(0);
    }
    await page.keyboard.press("Escape");

    const meta = await page.request.get("/rs/user/listMeta", {
      headers: { "X-PF-Frontend": "next" },
    });
    const { userAccess } = (await meta.json()) as {
      userAccess?: { update?: boolean; insert?: boolean };
    };
    expect(userAccess?.update).toBe(false);
    expect(userAccess?.insert).toBe(false);
  });
});

function usernameField(page: Page, format: UserFormat) {
  return page.getByLabel(label(format, "user.username"), { exact: true });
}

interface StoredUser {
  username?: string;
  description?: string;
  assignedGroups?: { id: number }[];
  userRights?: unknown[];
  lastLogin?: string;
}

/**
 * The id of the user [username], through the autocompletion every user picker uses. Its hits carry the
 * display name only, so each is confirmed by reading the user.
 */
async function userIdOf(
  request: APIRequestContext,
  username: string
): Promise<number> {
  const response = await request.get(
    `/rs/user/autosearch?search=${encodeURIComponent(username)}`,
    { headers: { "X-PF-Frontend": "next" } }
  );
  const hits = (await response.json()) as { id: number }[];
  for (const hit of hits) {
    if ((await storedUser(request, hit.id)).username === username)
      return hit.id;
  }
  throw new Error(`No user ${username} found`);
}

/** The user as its page's DTO — what a write has to be given in full. */
async function storedUser(
  request: APIRequestContext,
  id: number
): Promise<StoredUser & Record<string, unknown>> {
  const response = await request.get(`/rs/user/${id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  if (!response.ok()) {
    throw new Error(`Could not read user ${id}: HTTP ${response.status()}`);
  }
  return await response.json();
}

/** Writes the description back as it was, bypassing the page under test. */
async function restoreDescription(
  request: APIRequestContext,
  id: number,
  description: string | undefined
): Promise<void> {
  const status = await request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  const current = await storedUser(request, id);
  await request.put("/rs/user/saveorupdate", {
    headers: {
      "X-PF-Frontend": "next",
      "X-PF-CSRF-Token": csrfToken,
      "Content-Type": "application/json",
    },
    data: { data: { ...current, description: description ?? null } },
  });
}
