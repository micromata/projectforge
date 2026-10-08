import type { APIRequestContext, Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import { waitForRow } from "./fixtures/list-table";
import {
  insert,
  markAsDeleted,
  writeHeaders,
  type SeededProject,
} from "./fixtures/seed";
import { PROJECT_PAGE } from "../components/features/project/project.page";
import { PROJEKT_METADATA } from "../lib/metadata/projekt.generated";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The project (Projekt) page of projectforge-next — list and form, both rendered from PROJECT_PAGE.
 *
 * Everything it asserts on is the project `seededProject` created (see fixtures/seed.ts), a project of
 * the run's own seeded customer: the list of this database is a production copy, so no project of it
 * may be named in the source.
 *
 * `ProjektDO` is historizable, so the seeded project cannot be removed cleanly — it is marked deleted
 * at the end (afterAll), which keeps the row but takes it out of every default list. The cost 2 unit
 * one test creates stays with it.
 */
test.describe("project page", { tag: "@lane-customer" }, () => {
  let project: SeededProject;

  test.beforeEach(async ({ loggedInPage: page, seededProject }) => {
    project = seededProject;
    // The list filter is stored per user and per entity: a term or status left behind by another run
    // would decide whether the seeded project is in the list at all.
    await page.request
      .get("/rs/project/filter/reset", {
        headers: { "X-PF-Frontend": "next" },
      })
      .catch(() => undefined);
  });

  test("shows the declared columns under the labels of ProjektDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/project");

    await expect(
      page.getByRole("heading", {
        name: label(format, "fibu.projekt.title.list"),
      })
    ).toBeVisible({ timeout: 30_000 });

    // Anchored at the start rather than `exact`: the accessible name of a header includes its filter
    // button and its resize handle ("Name Filter Spaltenbreite ändern"), while a plain substring match
    // would let "Name" of the project find "Customer's name" (see order.spec.ts).
    for (const column of PROJECT_PAGE.columns) {
      const key = columnHeaderKeyOf(column, PROJEKT_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: new RegExp(`^${escapeRegExp(label(format, key))}(\\s|$)`),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("starts with the projects not ended", async ({ loggedInPage: page }) => {
    // The default of a user without a stored filter (ProjectEntityRest.newMagicFilter), sent with the
    // very first list request — a project without status counts as not ended.
    const request = page.waitForRequest(
      (candidate) =>
        candidate.url().includes("/rs/project/list") &&
        candidate.method() === "POST"
    );
    await goto(page, "/project");
    const body = JSON.parse((await request).postData() ?? "{}") as {
      entries?: { field: string; value: { values?: string[] } }[];
    };
    expect(
      body.entries?.find((entry) => entry.field === "listType")?.value.values
    ).toEqual(["notEnded"]);
  });

  test("narrows the list to the searched project and opens it by its row", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/project");
    await page
      .getByPlaceholder(format.t("filter.searchList"))
      .fill(project.suffix);
    const row = await waitForRow(page, project.name, 30_000);

    await row.click();

    await expect(page).toHaveURL(new RegExp(`/project/${project.id}$`));
    await expect(nameField(page, format)).toHaveValue(project.name);
    // The number reads as the cost number of the customer's project: "5.<customer>." before the box.
    await expect(
      page.getByText(`5.${String(project.customer.nummer).padStart(3, "0")}.`, {
        exact: true,
      })
    ).toBeVisible();
    await expect(
      page.getByLabel(
        `${format.t("fibu.projekt.nummer")}: ${format.t("fibu.projekt.nummer")}`,
        { exact: true }
      )
    ).toHaveValue(String(project.nummer).padStart(2, "0"));
  });

  test("creates the cost 2 unit of a checked cost 2 type", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const before = await storedProject(page.request, project.id);
    const candidate = before.kost2Arts?.find((art) => !art.existsAlready);
    test.skip(
      candidate == null,
      "No cost 2 type left that the project does not have."
    );
    await goto(page, `/project/${project.id}`);
    await expect(nameField(page, format)).toHaveValue(project.name, {
      timeout: 30_000,
    });

    await page
      .locator(`[data-kost2-art="${candidate!.id}"]`)
      .getByRole("checkbox")
      .click();
    await page
      .getByRole("button", { name: format.t("save"), exact: true })
      .click();

    await expect(
      page.getByText(format.t("message.successfullChanged"))
    ).toBeVisible();
    // Read back through the API: the cost 2 unit exists, and the project kept its number and name —
    // a hand built save posts the form's values *as* the DTO.
    const after = await storedProject(page.request, project.id);
    expect(
      after.kost2Arts?.find((art) => art.id === candidate!.id)?.existsAlready
    ).toBe(true);
    expect(after.nummer).toBe(project.nummer);
    expect(after.name).toBe(project.name);
  });

  test("deactivates a cost 2 unit when unchecked and reactivates it", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const before = await storedProject(page.request, project.id);
    const candidate = before.kost2Arts?.find((art) => art.active);
    test.skip(candidate == null, "The project has no active cost 2 unit.");
    const toggleAndSave = async (checked: boolean) => {
      await goto(page, `/project/${project.id}`);
      await expect(nameField(page, format)).toHaveValue(project.name, {
        timeout: 30_000,
      });
      const box = page
        .locator(`[data-kost2-art="${candidate!.id}"]`)
        .getByRole("checkbox");
      // The form still shifts while its last fields settle, so a click can land beside the box: the
      // target state is set, and set again until it holds.
      await expect(async () => {
        await box.setChecked(checked, { timeout: 2_000 });
        await expect(box).toBeChecked({ checked, timeout: 1_000 });
      }).toPass({ timeout: 15_000 });
      await page
        .getByRole("button", { name: format.t("save"), exact: true })
        .click();
      await expect(
        page.getByText(format.t("message.successfullChanged"))
      ).toBeVisible();
      return (await storedProject(page.request, project.id)).kost2Arts?.find(
        (art) => art.id === candidate!.id
      );
    };

    // Unchecked: the cost 2 unit is kept, but non-active (ProjectEntityRest.onAfterSaveOrUpdate).
    const deactivated = await toggleAndSave(false);
    expect(deactivated?.existsAlready).toBe(true);
    expect(deactivated?.active).toBe(false);
    // Checked again: the same cost 2 unit is active again.
    const reactivated = await toggleAndSave(true);
    expect(reactivated?.existsAlready).toBe(true);
    expect(reactivated?.active).toBe(true);
  });

  test("rejects a number the customer already has for another project", async ({
    seedRequest,
  }) => {
    // The backend's own check (ProjectEntityRest.validate), answered as a field error rather than the
    // database's unique constraint.
    await expect(
      insert(seedRequest, "project", {
        nummer: project.nummer,
        name: `${project.name} duplicate`,
        customer: { id: project.customer.id },
      })
    ).rejects.toThrow(/Number already exists|Nummer bereits/);
  });

  test("filters the list by the status", async ({ seedRequest }) => {
    // The seeded project has no status: "not ended" and NONE find it, "ended" does not.
    const names = async (values: string[]) =>
      (await listProjects(seedRequest, project.suffix, values)).map(
        (row) => row.name
      );
    expect(await names(["notEnded"])).toContain(project.name);
    expect(await names(["NONE"])).toContain(project.name);
    expect(await names(["ENDED"])).not.toContain(project.name);
    // Several values are OR-combined.
    expect(await names(["ENDED", "NONE"])).toContain(project.name);
  });

  test("mass update creates and deactivates cost 2 types, the list filters and strikes them", async ({
    loggedInPage: page,
  }) => {
    const before = await storedProject(page.request, project.id);
    const [candidate, absent] = (before.kost2Arts ?? []).filter(
      (art) => !art.existsAlready
    );
    test.skip(
      candidate == null || absent == null,
      "The project has every cost 2 type already."
    );
    const art = (id: number) =>
      storedProject(page.request, project.id).then((p) =>
        p.kost2Arts?.find((a) => a.id === id)
      );

    // Create/activate: the project gets an active cost 2 unit of the type.
    const created = await massUpdateKost2Arts(page.request, project, {
      textValue: String(candidate!.id),
      append: true,
    });
    expect(created.modifiedCounter).toBe(1);
    expect(await art(candidate!.id)).toMatchObject({
      existsAlready: true,
      active: true,
    });

    // The "active" filter finds it, a type it has no unit of does not; the "non-active/missing" filter the
    // other way round, and for both types picked (one of them missing).
    const filtered = async (field: string, ...artIds: number[]) =>
      (
        await listProjects(
          page.request,
          project.suffix,
          [],
          [{ field, value: { values: artIds.map(String) } }]
        )
      ).map((row) => row.name);
    const active = (...artIds: number[]) =>
      filtered("kost2.kost2Art.id", ...artIds);
    const notActive = (...artIds: number[]) =>
      filtered("kost2ArtsNotActive", ...artIds);
    expect(await active(candidate!.id)).toContain(project.name);
    expect(await active(absent!.id)).not.toContain(project.name);
    expect(await notActive(candidate!.id)).not.toContain(project.name);
    expect(await notActive(absent!.id)).toContain(project.name);
    expect(await notActive(candidate!.id, absent!.id)).toContain(project.name);

    // Deactivating a type the project has no unit of is a no-op, the other one is set non-active.
    const noop = await massUpdateKost2Arts(page.request, project, {
      textValue: String(absent!.id),
      delete: true,
    });
    expect(noop.modifiedCounter).toBe(0);
    expect((await art(absent!.id))?.existsAlready).toBe(false);
    const deactivated = await massUpdateKost2Arts(page.request, project, {
      textValue: String(candidate!.id),
      delete: true,
    });
    expect(deactivated.modifiedCounter).toBe(1);
    expect(await art(candidate!.id)).toMatchObject({
      existsAlready: true,
      active: false,
    });
    // Non-active counts as missing.
    expect(await active(candidate!.id)).not.toContain(project.name);
    expect(await notActive(candidate!.id)).toContain(project.name);

    // The list cell strikes the non-active type through.
    await goto(page, `/project?q=${encodeURIComponent(project.suffix)}`);
    const row = await waitForRow(page, project.name);
    await expect(
      row
        .locator(".line-through")
        .getByText(String(candidate!.id).padStart(2, "0"), { exact: true })
    ).toBeVisible();

    // Its tooltip lists the types with their names, one per line — also hovered on the cell's padding,
    // where the pointer misses the element declaring it (see useOverflowTooltip).
    const cell = row.locator("td", { has: page.locator(".line-through") });
    // Scrolled into view first: the scroll a hover does itself lands after its pointerover and
    // dismisses the pending tooltip (see useOverflowTooltip's onScroll).
    await cell.scrollIntoViewIfNeeded();
    await cell.hover({ position: { x: 2, y: 2 } });
    const tooltip = page.locator("[data-slot=tooltip-content]").first();
    await expect(tooltip).toBeVisible();
    if (candidate!.name) await expect(tooltip).toContainText(candidate!.name);
  });

  test.afterAll(async ({ seedRequest, seededProject }) => {
    await markAsDeleted(seedRequest, "project", seededProject.id).catch(
      () => undefined
    );
  });
});

interface StoredProject {
  name?: string;
  nummer?: number;
  kost2Arts?: {
    id: number;
    name?: string;
    existsAlready?: boolean;
    active?: boolean;
  }[];
}

/** The project as its edit page's DTO, cost 2 types included. */
async function storedProject(
  request: APIRequestContext,
  id: number
): Promise<StoredProject> {
  const response = await request.get(`/rs/project/${id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  if (!response.ok()) {
    throw new Error(`Could not read project ${id}: HTTP ${response.status()}`);
  }
  return await response.json();
}

/** The list rows for a search term under the given `listType` values. */
async function listProjects(
  request: APIRequestContext,
  searchString: string,
  listTypes: string[],
  entries: { field: string; value: { values: string[] } }[] = []
): Promise<{ name?: string }[]> {
  const response = await request.post("/rs/project/list", {
    headers: await writeHeaders(request),
    data: {
      searchString,
      entries: [
        { field: "listType", value: { values: listTypes } },
        ...entries,
      ],
    },
  });
  if (!response.ok()) {
    throw new Error(`Could not list projects: HTTP ${response.status()}`);
  }
  const body = (await response.json()) as { resultSet?: { name?: string }[] };
  return body.resultSet ?? [];
}

/**
 * Runs the project mass update on the given project alone, with the cost 2 types parameter only — the way
 * the list does it: register the list's ids (`startSelection`), tick the project (`select`), then `update`.
 */
async function massUpdateKost2Arts(
  request: APIRequestContext,
  project: SeededProject,
  param: { textValue: string; append?: boolean; delete?: boolean }
): Promise<{ modifiedCounter: number; errorCounter: number }> {
  const post = async (url: string, data: unknown) => {
    const response = await request.post(url, {
      headers: await writeHeaders(request),
      data,
    });
    if (!response.ok()) {
      throw new Error(
        `${url}: HTTP ${response.status()} ${await response.text()}`
      );
    }
    return response.json();
  };
  await post("/rs/project/startSelection", {
    searchString: project.suffix,
    entries: [{ field: "listType", value: { values: [] } }],
  });
  try {
    await post("/rs/projectSelected/select", { selectedIds: [project.id] });
    return await post("/rs/projectSelected/update", { kost2Arts: param });
  } finally {
    // The selection stays in the session otherwise, and the list would restore it on its next visit:
    // selection mode, and the late `select` re-laying out the table under a pointer hovering a cell.
    await request.get("/rs/projectSelected/cancel", {
      headers: { "X-PF-Frontend": "next" },
    });
  }
}

/** The name field, by the label ProjektDO gives it. */
function nameField(page: Page, format: UserFormat) {
  return page.getByLabel(label(format, "fibu.projekt.name"), { exact: true });
}

/** Escapes a label for use inside a `RegExp` — a label may carry brackets or dots. */
function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
