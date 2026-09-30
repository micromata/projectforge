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
test.describe("project page", () => {
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
      page.getByRole("heading", { name: format.t("fibu.projekt.title.list") })
    ).toBeVisible({ timeout: 30_000 });

    // Exact: "Name" of the project would otherwise also match "Customer's name".
    for (const column of PROJECT_PAGE.columns) {
      const key = columnHeaderKeyOf(column, PROJEKT_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: label(format, key),
          exact: true,
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
    const toggleAndSave = async () => {
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
      return (await storedProject(page.request, project.id)).kost2Arts?.find(
        (art) => art.id === candidate!.id
      );
    };

    // Unchecked: the cost 2 unit is kept, but non-active (ProjectEntityRest.onAfterSaveOrUpdate).
    const deactivated = await toggleAndSave();
    expect(deactivated?.existsAlready).toBe(true);
    expect(deactivated?.active).toBe(false);
    // Checked again: the same cost 2 unit is active again.
    const reactivated = await toggleAndSave();
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

  test.afterAll(async ({ seedRequest, seededProject }) => {
    await markAsDeleted(seedRequest, "project", seededProject.id).catch(
      () => undefined
    );
  });
});

interface StoredProject {
  name?: string;
  nummer?: number;
  kost2Arts?: { id: number; existsAlready?: boolean; active?: boolean }[];
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
  listTypes: string[]
): Promise<{ name?: string }[]> {
  const response = await request.post("/rs/project/list", {
    headers: await writeHeaders(request),
    data: {
      searchString,
      entries: [{ field: "listType", value: { values: listTypes } }],
    },
  });
  if (!response.ok()) {
    throw new Error(`Could not list projects: HTTP ${response.status()}`);
  }
  const body = (await response.json()) as { resultSet?: { name?: string }[] };
  return body.resultSet ?? [];
}

/** The name field, by the label ProjektDO gives it. */
function nameField(page: Page, format: UserFormat) {
  return page.getByLabel(label(format, "fibu.projekt.name"), { exact: true });
}
