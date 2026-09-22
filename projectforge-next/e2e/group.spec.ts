import type { APIRequestContext, Page } from "@playwright/test";
import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import { waitForRow } from "./fixtures/list-table";
import { GROUP_PAGE } from "../components/features/group/group.page";
import { GROUP_METADATA } from "../lib/metadata/group.generated";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";
import type { SeededGroup } from "./fixtures/seed";

/**
 * The group page of projectforge-next — list and form, both rendered from GROUP_PAGE.
 *
 * Everything it asserts on is the group `seededGroup` created (see fixtures/seed.ts): the list of this
 * database is a production copy, so no group of it may be named in the source, and on a fresh database
 * only the system groups exist. That group is local (`localGroup: true`), which the status filter case
 * relies on.
 *
 * The one writing case changes `organization` and puts the stored value back afterwards, since the
 * group is shared with task-wizard.spec.ts. Nothing here creates a group of its own: `GroupDO` is
 * historizable, so a group cannot be removed again (see fixtures/seed.ts).
 */
test.describe("group page", () => {
  let group: SeededGroup;

  test.beforeEach(async ({ loggedInPage: page, seededGroup }) => {
    group = seededGroup;
    // The filter is stored per user and per entity: a status left behind by another run (or by the
    // case below) would decide whether the seeded group is in the list at all.
    await page.request
      .get("/rs/group/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("shows the declared columns under the labels of GroupDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/group");

    await expect(
      page.getByRole("heading", { name: format.t("group.title.list._") })
    ).toBeVisible({ timeout: 30_000 });

    // Against the metadata, never against literals: every column label is the `i18nKey` of the field
    // in GroupDO, which is exactly what the declaration does not repeat.
    for (const column of GROUP_PAGE.columns) {
      // `ldapValues` is only a column where the backend says posix accounts are configured
      // (`ColumnBase.visible`), which no test may decide for the installation it runs against.
      if ("visible" in column) continue;
      const key = columnHeaderKeyOf(column, GROUP_METADATA);
      await expect(
        page.getByRole("columnheader", { name: label(format, key) }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("narrows the list to the searched group and opens it by its row", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/group");
    // The run's own suffix, not the whole name: "ZZ e2e group" is the name of every group an earlier
    // run left, and the backend matches a row on any word of the term.
    await page
      .getByPlaceholder(format.t("filter.searchList"))
      .fill(group.suffix);
    const row = await waitForRow(page, group.name, 30_000);

    await row.click();

    await expect(page).toHaveURL(new RegExp(`/group/${group.id}$`));
    await expect(nameField(page, format)).toHaveValue(group.name);
  });

  test("prefills the form with the stored group", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const stored = await storedGroup(page.request, group.id);
    // A deep link rather than a click from the list: it is what proves the SPA shell map covers
    // `group/[id]` — under `output: 'export'` an unknown route lands on Next's 404.
    await goto(page, `/group/${group.id}`);

    await expect(nameField(page, format)).toHaveValue(stored.name ?? "", {
      timeout: 30_000,
    });
    // Against the response, not against literals: which fields the seed set is the fixture's business,
    // and Spring omits an empty one entirely (`JsonInclude.Include.NON_NULL`).
    await expect(organizationField(page, format)).toHaveValue(
      stored.organization ?? ""
    );
    await expect(
      page.getByLabel(label(format, "description"), { exact: true })
    ).toHaveValue(stored.description ?? "");
    // By role, not by label: the field's hint button carries the same text in its `aria-label`
    // („Hinweis: …"), which `getByLabel` matches as a substring.
    const localGroup = page.getByRole("checkbox", {
      name: format.t("group.localGroup._"),
    });
    if (stored.localGroup) await expect(localGroup).toBeChecked();
    else await expect(localGroup).not.toBeChecked();
    // Computed by the backend from the members and never written back — the form's counterpart of
    // `UIReadOnlyField("emails")`. The seeded group has no members, so this one is empty; that it
    // carries what the response carries is the case below.
    const emails = page.getByLabel(label(format, "address.emails"), {
      exact: true,
    });
    await expect(emails).toBeDisabled();
    await expect(emails).toHaveValue(stored.emails ?? "");
  });

  test("shows the mail addresses of the members", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    // A group of the database that has members, found at runtime: the seeded group has none (it grants
    // rights, nobody logs in as it), and no group of this production copy may be named in the source.
    const withMembers = await groupWithMembers(page.request);
    test.skip(
      withMembers === null,
      "no group with a member whose address is known"
    );
    const stored = await storedGroup(page.request, withMembers!);

    // Both halves in one: `Group.populateEmails` runs on read at all (it used to run only while the
    // server built the layout, which a hand built form never asks for — the field stayed empty), and the
    // form shows exactly what came with the entity.
    expect(stored.emails ?? "").toContain("@");
    await goto(page, `/group/${withMembers}`);
    await expect(
      page.getByLabel(label(format, "address.emails"), { exact: true })
    ).toHaveValue(stored.emails ?? "", { timeout: 30_000 });
  });

  test("saves a change and returns to the list", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const stored = await storedGroup(page.request, group.id);
    const changed = `${group.name} [pf-e2e]`;
    await goto(page, `/group/${group.id}`);
    const organization = organizationField(page, format);
    await expect(organization).toHaveValue(stored.organization ?? "", {
      timeout: 30_000,
    });

    try {
      await organization.fill(changed);
      await page
        .getByRole("button", { name: format.t("save"), exact: true })
        .click();

      await expect(
        page.getByText(format.t("message.successfullChanged"))
      ).toBeVisible();
      await expect(page).toHaveURL(/\/group$/);
      // Read back through the API: the assertion is on what was stored, not on what the list cached.
      const after = await storedGroup(page.request, group.id);
      expect(after.organization).toBe(changed);
      // And nothing else was lost on the way. A hand built save posts the form's values *as* the DTO
      // and `BaseDTO.copy` copies field by field, so a value the form doesn't carry would be cleared —
      // `ldapValues` is written by the LDAP sync and shown nowhere in this form (see group-schema.ts).
      expect(after.name).toBe(stored.name);
      expect(after.localGroup).toBe(stored.localGroup);
      expect(after.ldapValues ?? null).toBe(stored.ldapValues ?? null);
    } finally {
      await restoreOrganization(page.request, group.id, stored.organization);
    }
    const restored = await storedGroup(page.request, group.id);
    expect(restored.organization ?? null).toBe(stored.organization ?? null);
  });

  test("filters the list by the status of a group", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/group");
    await page
      .getByPlaceholder(format.t("filter.searchList"))
      .fill(group.suffix);
    await waitForRow(page, group.name, 30_000);

    // `type` is a `defaultFilter`, so its pill is on the row without being added first — labelled with
    // the backend's own word for it (`GroupPagesRest.addMagicFilterElements`).
    await page
      .getByRole("button", {
        name: format.t("filter.editEntry", { arg0: format.t("status") }),
      })
      .click();
    // The pill popover applies live and has no save button (see filter-pill-shell): picking a value
    // fires the list request by itself, so the wait is armed before the click and matched on the enum
    // it carries. A LIST field's choices lie open in the popover rather than behind a select of their own.
    const request = page.waitForRequest(
      (candidate) =>
        candidate.url().includes("/rs/group/list") &&
        candidate.method() === "POST" &&
        (candidate.postData() ?? "").includes("LOCAL_GROUP")
    );
    await page
      .locator('[data-slot="popover-content"]')
      .getByRole("option", {
        name: format.t("group.localGroup._"),
        exact: true,
      })
      .click();

    // `GroupTypeFilter` matches on the enum name, and a value in the wrong shape would be dropped
    // silently — the list would look right and simply not filter.
    const body = JSON.parse((await request).postData() ?? "{}") as {
      entries: { field: string; value: { values?: string[] } }[];
    };
    expect(
      body.entries.find((entry) => entry.field === "type")?.value.values
    ).toEqual(["LOCAL_GROUP"]);
    // The seeded group is local, so it survives its own filter — which says more than a row count.
    await waitForRow(page, group.name, 30_000);
  });

  // The status filter is stored per user and per entity, so it must not leak into another spec.
  test.afterAll(async ({ request }) => {
    await request
      .get("/rs/group/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });
});

/**
 * The same list for a user who may read it but not add to it — the rights case of every hand-built list
 * (see useEditTargets), with `group` as the entity that has it: `GroupDao.hasUserSelectAccess` answers
 * true for everyone, while inserting is the admin group's (`GroupDao.hasAccess`), so `listMeta` answers
 * `userAccess.insert: false` for a plain user. Legacy leaves the create entry out of the page menu in
 * that case (`AbstractPagesRest.createListLayout`); next offered its "+" to everyone.
 *
 * The row click goes with it (`userAccess.update`, which `GroupPagesRest.listUpdateAccess` answers for
 * administrators only — Wicket's `GroupListPage` renders the name as a plain label without it). That one
 * is asserted on the response rather than on a row: this user's list is empty, `GroupDao` shows them no
 * group at all, so there would be nothing to click either way.
 *
 * Skipped rather than failed where the account is missing: not every instance has every role (see
 * fixtures/credentials.ts).
 */
test.describe("group list without the insert right", () => {
  test.skip(
    !hasRole("normalo-user"),
    "no normalo-user in this instance's testAccounts.txt"
  );

  test("leaves the add button out", async ({ page }) => {
    // Not `loggedInPage`: that fixture logs in as the account with every right, which is the account
    // that must *not* be looked at here.
    await login(page, "/next/", "normalo-user");
    const format = await userFormat(page);
    await goto(page, "/group");
    // The page itself is there — this user may read the list (`userAccess.read`), it is only empty for
    // them, so the assertion below is about the button and not about a page that never rendered.
    await expect(
      page.getByRole("heading", { name: format.t("group.title.list._") })
    ).toBeVisible({ timeout: 30_000 });

    // Gone, not disabled: there is nothing this user could do to enable it, and its `N` shortcut goes
    // with it (see AddEntryButton, which owns both).
    await expect(
      page.getByRole("link", { name: format.t("menu.addNewEntry") })
    ).toHaveCount(0);
  });

  test("is not offered as clickable rows either", async ({ page }) => {
    await login(page, "/next/", "normalo-user");
    // The flag both list pages hang their row click on — read here, because this account sees no group
    // to click. The counter-case is the whole spec above: `loggedInPage` is an administrator and opens
    // the seeded group by its row.
    const meta = await page.request.get("/rs/group/listMeta", {
      headers: { "X-PF-Frontend": "next" },
    });
    expect(meta.ok()).toBe(true);
    const { userAccess } = (await meta.json()) as {
      userAccess?: { update?: boolean; insert?: boolean };
    };
    expect(userAccess?.update).toBe(false);
    expect(userAccess?.insert).toBe(false);
  });

  test("keeps it for a user who may insert", async ({ loggedInPage: page }) => {
    // The counter-case, so the one above cannot pass on a toolbar that stopped rendering the button
    // altogether.
    const format = await userFormat(page);
    await goto(page, "/group");
    await expect(
      page.getByRole("link", { name: format.t("menu.addNewEntry") })
    ).toBeVisible({ timeout: 30_000 });
  });
});

/** The group as its page's DTO — what a write has to be given in full. */
async function storedGroup(
  request: APIRequestContext,
  id: number
): Promise<{
  name?: string;
  organization?: string;
  description?: string;
  localGroup?: boolean;
  ldapValues?: string;
  emails?: string;
}> {
  const response = await request.get(`/rs/group/${id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  if (!response.ok()) {
    throw new Error(`Could not read group ${id}: HTTP ${response.status()}`);
  }
  return await response.json();
}

/**
 * The id of a group whose members have mail addresses, or null if this installation has none.
 *
 * Found through the list, whose rows carry `assignedUsers` (`GroupPagesRest.createListLayout`), and
 * then confirmed on the entity: a member without an address contributes nothing to `emails`, so only
 * reading it settles whether the case has anything to assert on. Few candidates on purpose — one hit
 * is enough and every candidate is a request.
 */
async function groupWithMembers(
  request: APIRequestContext
): Promise<number | null> {
  const response = await request.post("/rs/group/list", {
    headers: await writeHeaders(request),
    data: { searchString: "" },
  });
  if (!response.ok()) return null;
  const { resultSet = [] } = (await response.json()) as {
    resultSet?: { id?: number; assignedUsers?: unknown[] }[];
  };
  const candidates = resultSet
    .filter((row) => (row.assignedUsers?.length ?? 0) > 0)
    .map((row) => row.id)
    .filter((id): id is number => typeof id === "number")
    .slice(0, 5);
  for (const id of candidates) {
    if ((await storedGroup(request, id)).emails?.includes("@")) return id;
  }
  return null;
}

/** A POST is a write as far as the backend is concerned and therefore needs the CSRF token. */
async function writeHeaders(
  request: APIRequestContext
): Promise<Record<string, string>> {
  const status = await request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  return {
    "X-PF-Frontend": "next",
    "X-PF-CSRF-Token": csrfToken,
    "Content-Type": "application/json",
  };
}

/** Writes the organization back as the fixture handed it over, bypassing the page under test. */
async function restoreOrganization(
  request: APIRequestContext,
  id: number,
  organization: string | undefined
): Promise<void> {
  // The whole DTO as it stands now, with `organization` explicitly null where the seed had none:
  // `saveorupdate` saves what it is given, so a field merely left out would keep the changed value.
  const current = await request.get(`/rs/group/${id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  await request.put("/rs/group/saveorupdate", {
    headers: await writeHeaders(request),
    data: {
      data: {
        ...((await current.json()) as Record<string, unknown>),
        organization: organization ?? null,
      },
    },
  });
}

/** The two text fields the cases read, by the labels GroupDO gives them. */
function nameField(page: Page, format: UserFormat) {
  return page.getByLabel(label(format, "name"), { exact: true });
}

function organizationField(page: Page, format: UserFormat) {
  return page.getByLabel(label(format, "organization"), { exact: true });
}
