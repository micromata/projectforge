import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { CONTRACT_METADATA } from "../lib/metadata/contract.generated";
import { CONTRACT_PAGE } from "../components/features/contract/contract.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";
import { withDeclaredColumns } from "../components/data-table/column-order";
import { writeHeaders } from "./fixtures/seed";

/**
 * The contracts against the live backend (see CONTRACT_PAGE and ContractEntityRest).
 *
 * The database may be a copy of production: nothing is written. The save test intercepts the save request, and the
 * list filter of the account is left as it is.
 */
test.describe("contract", { tag: "@parallel" }, () => {
  test("shows the declared columns under the labels of ContractDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/contract");

    await expect(
      page.getByRole("heading", { name: format.t(CONTRACT_PAGE.titleKey) })
    ).toBeVisible();
    for (const column of CONTRACT_PAGE.columns) {
      if (column.hiddenByDefault) continue;
      const key = columnHeaderKeyOf(column, CONTRACT_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: new RegExp(`^${escape(label(format, key))}`),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("offers the configured contract types to the account", async ({
    loggedInPage: page,
  }) => {
    // The types are the configuration's (orga.contractTypes), possibly none; what is asserted is the answer's shape.
    const response = await page.request.get("/rs/contract/types", {
      headers: { "X-PF-Frontend": "next" },
    });
    expect(response.status()).toBe(200);
    const types = (await response.json()) as unknown[];
    expect(Array.isArray(types)).toBe(true);
    for (const type of types) {
      expect(type).toEqual({
        value: expect.any(String),
        label: expect.any(String),
      });
    }
  });

  test("posts a new contract on save", async ({ loggedInPage: page }) => {
    const format = await userFormat(page);
    let saveAttempted = false;
    await page.route("**/rs/contract/{insert,update}*", (route) => {
      saveAttempted = true;
      return route.abort();
    });

    // Today is preset as the date by the backend (ContractEntityRest.newBaseDO); the title is mandatory. Both fields
    // complete from other contracts, so they are comboboxes (SuggestInput).
    await goto(page, "/contract/new");
    await page
      .getByRole("combobox", { name: label(format, "title") })
      .first()
      .fill("e2e contract");
    await page
      .getByRole("combobox", {
        name: label(format, "legalAffaires.contract.coContractorA"),
      })
      .first()
      .fill("e2e co-contractor");
    await page.getByRole("button", { name: format.t("create") }).click();

    await expect
      .poll(() => saveAttempted, { message: "the save reaches the backend" })
      .toBe(true);
  });

  test("shows the declared columns over a layout the React grid stored", async ({
    loggedInPage: page,
  }) => {
    // The React grid kept its layout under the same category and with its own ids (`statusAsString`, not `status`).
    // Served from here rather than stored, so the account's own layout is neither read nor overwritten.
    const reactLayout = {
      columnOrder: ["statusAsString", "title", "number", "date"],
      columnSizing: { statusAsString: 120, title: 200 },
      sorting: [{ id: "statusAsString", desc: false }],
    };
    await page.route("**/rs/contract/columnStates", (route) =>
      route.fulfill({ json: reactLayout })
    );
    await page.route("**/rs/contract/setColumnStates", (route) =>
      route.fulfill({ body: "OK" })
    );
    const listCall = page.waitForRequest(
      (request) =>
        request.url().includes("/rs/contract/list") &&
        request.method() === "POST"
    );

    await goto(page, "/contract");

    // The React id is dropped, and the columns its order lacks keep their declared neighbours rather than trailing.
    const declaredIds = CONTRACT_PAGE.columns
      .filter((column) => !column.hiddenByDefault)
      .map(columnIdOf);
    const expected = withDeclaredColumns(
      reactLayout.columnOrder,
      declaredIds
    ).filter((id) => declaredIds.includes(id));
    await expect
      .poll(() =>
        page
          .locator("thead [data-column-id]")
          .evaluateAll((cells) =>
            cells.map((cell) => cell.getAttribute("data-column-id"))
          )
      )
      .toEqual(expected);

    // Nor does the list sort by the React id: the page's default (number, descending) applies.
    const sent = (await listCall).postDataJSON() as {
      sortProperties?: { property: string }[];
    };
    expect(sent.sortProperties?.map((entry) => entry.property)).toEqual([
      "number",
    ]);
  });

  test("shows a year filter the React list stored, so it can be removed", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    // The React list offered the year as a pinned filter, so the remembered filter of many users carries it. Served
    // from here, and the list calls answered here too: they would store the filter as the account's current one.
    await page.route("**/rs/contract/listMeta*", async (route) => {
      const json = await (await route.fetch()).json();
      json.filter = {
        ...(json.filter ?? {}),
        entries: [{ field: "year", value: { value: "2020" } }],
      };
      return route.fulfill({ json });
    });
    const sentYears: (string | undefined)[] = [];
    await page.route("**/rs/contract/{list,listPage}*", (route) => {
      // The glob matches listMeta, too.
      if (route.request().method() !== "POST") return route.fallback();
      const body = route.request().postDataJSON() as {
        entries?: { field: string; value?: { value?: string } }[];
        filter?: { entries?: { field: string; value?: { value?: string } }[] };
      };
      const entries = body.entries ?? body.filter?.entries ?? [];
      sentYears.push(
        entries.find((entry) => entry.field === "year")?.value?.value
      );
      return route.fulfill({ json: { resultSet: [], totalSize: 0 } });
    });

    await goto(page, "/contract");

    // The backend still narrows by it (ContractEntityRest.preProcessMagicFilter), so it must be on the filter row.
    const remove = page.getByRole("button", {
      name: format.t("filter.removeEntry", { arg0: format.t("calendar.year") }),
    });
    await expect(remove).toBeVisible();
    await expect.poll(() => sentYears[0]).toBe("2020");

    await remove.click();
    // Asked again without it.
    await expect
      .poll(() => sentYears.length > 1 && sentYears.at(-1) === undefined)
      .toBe(true);
  });

  test("opens a clone as a new contract without the number", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    // Any stored contract: the clone (`cloneData`) only prepares the copy, nothing is saved. Asked without storing the
    // filter, so the account's list filter stays as it is.
    const response = await page.request.post(
      "/rs/contract/list?doNotStore=true",
      { headers: await writeHeaders(page.request), data: { entries: [] } }
    );
    expect(response.ok()).toBe(true);
    const contracts = (
      (await response.json()) as {
        resultSet: { id: number; title?: string; number?: number }[];
      }
    ).resultSet.filter((contract) => contract.title && contract.number);
    test.skip(!contracts.length, "the database has no contract to clone");
    const contract = contracts[0];

    await goto(page, `/contract/${contract.id}`);
    const number = page.getByLabel(
      label(format, "legalAffaires.contract.number"),
      { exact: true }
    );
    await expect(number).toHaveValue(String(contract.number));
    await page
      .getByRole("button", { name: format.t("clone"), exact: true })
      .click();

    await expect(page).toHaveURL(/\/contract\/new\?clone=1$/);
    // The clone button needs a stored entry: gone once the page has become the add page.
    await expect(
      page.getByRole("button", { name: format.t("clone"), exact: true })
    ).toHaveCount(0);
    // The number is handed out on saving (ContractEntityRest.prepareClone), the rest is the original's.
    await expect(number).toHaveValue("");
    await expect(
      page.getByRole("combobox", { name: label(format, "title") }).first()
    ).toHaveValue(contract.title!);
  });

  test("redirects the old React pages", async ({ loggedInPage: page }) => {
    await page.goto("/react/contract");
    await expect(page).toHaveURL(/\/next\/contract$/);
    await page.goto("/react/contract/edit");
    await expect(page).toHaveURL(/\/next\/contract\/new$/);
  });
});

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
