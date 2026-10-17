import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import {
  fetchEntity,
  insert,
  markAsDeleted,
  writeHeaders,
} from "./fixtures/seed";
import { formatDateInput } from "../lib/date-parse";
import { AVAILABILITY_METADATA } from "../lib/metadata/availability.generated";
import { AVAILABILITY_PAGE } from "../components/features/availability/availability.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The availability entries against the live backend (see AVAILABILITY_PAGE and AvailabilityEntityRest). Nothing
 * is written but the stored choice of the team vacation calendar, which is put back, and the two entries of the
 * substitutes' test, marked as deleted afterwards. The form test doesn't save; it posts a period ending before it
 * begins, which the server refuses before any write (AvailabilityEntityRest.validate).
 */
test.describe("availability", () => {
  test("shows the declared columns under the labels of AvailabilityDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/availability");

    await expect(
      page.getByRole("heading", {
        name: format.t(AVAILABILITY_PAGE.titleKey),
      })
    ).toBeVisible();
    for (const column of AVAILABILITY_PAGE.columns) {
      const key = columnHeaderKeyOf(column, AVAILABILITY_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: startsWith(label(format, key)),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("opens the team vacation calendar with the availability entries shown", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const headers = { "X-PF-Frontend": "next" };
    const initial = (await (
      await page.request.get("/rs/vacationExport/initial", { headers })
    ).json()) as {
      employees: { id: number }[];
      groups: { id: number }[];
      periodKind?: string;
      dayWidth?: string;
      absenceKinds: string[];
    };
    // The choice is stored per user by every view: the vacations only first, so only the url can turn on the rest.
    const store = async (absenceKinds: string[]) =>
      page.request.post("/rs/vacationExport/view", {
        headers: await writeHeaders(page.request),
        data: {
          employeeIds: initial.employees.map((e) => e.id),
          groupIds: initial.groups.map((g) => g.id),
          periodKind: initial.periodKind,
          dayWidth: initial.dayWidth,
          absenceKinds,
          from: "2030-01-01",
          until: "2030-01-01",
        },
      });
    try {
      expect((await store(["VACATION"])).ok()).toBe(true);
      await goto(page, "/availability");
      const link = page.getByRole("link", {
        name: format.t("vacation.export.title"),
      });
      await expect(link).toHaveAttribute(
        "href",
        /\/vacationExport\?absenceKinds=all$/
      );
      await link.click();
      // Applied once and dropped from the url, so a reload keeps a choice made on the page.
      await expect(page).toHaveURL(/\/vacationExport/);
      await expect(page).not.toHaveURL(/absenceKinds/);
      const kinds = page.getByRole("group", {
        name: format.t("vacation.export.absenceKinds._"),
      });
      for (const key of [
        "vacation._",
        "availability.status.notAvailable",
        "availability.status.reachableIfUrgent",
      ]) {
        await expect(
          kinds.getByRole("button", { name: format.t(key), exact: true })
        ).toHaveAttribute("aria-pressed", "true");
      }
    } finally {
      await store(initial.absenceKinds);
    }
  });

  test("links the vacation account, the vacation list and the availability entries with each other", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const linkTo = (name: string) =>
      page.getByRole("link", { name, exact: true });
    await goto(page, "/availability");
    await expect(linkTo(t("menu.vacation.leaveaccount"))).toHaveAttribute(
      "href",
      /\/vacationAccount$/
    );
    await goto(page, "/vacation");
    await expect(linkTo(t("menu.availability"))).toHaveAttribute(
      "href",
      /\/availability$/
    );
    // The account's actions only with an employee (a slot database's full access user has none).
    const account = (await (
      await page.request.get("/rs/vacationAccount", {
        headers: { "X-PF-Frontend": "next" },
      })
    ).json()) as { employee?: { id: number } };
    if (account.employee) {
      await goto(page, "/vacationAccount");
      await expect(linkTo(t("menu.availability"))).toHaveAttribute(
        "href",
        /\/availability$/
      );
    }
  });

  test("offers the configured types and the internal comment, keeps the period a period and refuses an end before the start", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const newEntry = async (query = "") =>
      (await (
        await page.request.get(`/rs/availability/newEntry${query}`, {
          headers: { "X-PF-Frontend": "next" },
        })
      ).json()) as {
        user?: { id: number; displayName?: string };
        availableTypes?: string[];
      };
    // The own user — also without an employee record (a freelancer), which the full access user may lack.
    const preset = await newEntry();
    expect(preset.user, "preset with the logged-in user").toBeTruthy();

    await goto(page, "/availability/new");
    await expect(
      page.getByText(preset.user!.displayName!).first()
    ).toBeVisible();
    // The user (and HR staff) get the internal comment.
    await expect(
      page.getByLabel(label(format, "availability.internalComment"), {
        exact: true,
      })
    ).toBeVisible();

    // The configured types.
    const types = preset.availableTypes ?? [];
    expect(types.length).toBeGreaterThan(0);
    await page
      .getByRole("combobox", { name: label(format, "availability.type") })
      .click();
    for (const type of types) {
      await expect(page.getByRole("option", { name: type })).toBeVisible();
    }
    await page.getByRole("option", { name: types[0] }).click();

    const year = new Date().getFullYear() + 3;
    const start = page.getByLabel(label(format, "availability.startDate"), {
      exact: true,
    });
    const end = page.getByLabel(label(format, "availability.endDate"), {
      exact: true,
    });
    await start.fill(formatDateInput(`${year}-02-10`, format.context));
    await start.press("Enter");
    // A start without an end makes it a one-day period (usePeriodClamp).
    await expect(end).toHaveValue(
      formatDateInput(`${year}-02-10`, format.context)
    );
    // An end before the start moves the start onto it: the form can't post such a period.
    await end.fill(formatDateInput(`${year}-02-09`, format.context));
    await end.press("Enter");
    await expect(start).toHaveValue(
      formatDateInput(`${year}-02-09`, format.context)
    );

    // Posted anyway, it's refused by the server before any write (AvailabilityEntityRest.validate), at the end date.
    const response = await page.request.put("/rs/availability/saveorupdate", {
      headers: await writeHeaders(page.request),
      data: {
        data: {
          user: { id: preset.user!.id },
          status: "NOT_AVAILABLE",
          startDate: `${year}-02-10`,
          endDate: `${year}-02-09`,
        },
      },
    });
    expect(response.status()).toBe(406);
    const errors = (
      (await response.json()) as {
        validationErrors: { fieldId?: string; message: string }[];
      }
    ).validationErrors;
    expect(
      errors.some((e) => e.fieldId === "endDate"),
      "a validation error at the end date"
    ).toBe(true);
  });

  test("shows the conflict of an entry whose substitute isn't available, with the substitute's absence", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const own = (
      (await (
        await page.request.get("/rs/availability/newEntry", {
          headers: { "X-PF-Frontend": "next" },
        })
      ).json()) as { user?: { id: number } }
    ).user;
    expect(own, "preset with the logged-in user").toBeTruthy();
    const users = (await (
      await page.request.get("/rs/user/autosearch?search=", {
        headers: { "X-PF-Frontend": "next" },
      })
    ).json()) as { id: number; displayName?: string }[];
    const substitute = users.find((u) => u.id !== own!.id);
    test.skip(!substitute, "no other user to stand in");
    // Years ahead, so no stored entry of either is in the way; the full access user may enter both (HR right).
    const year = new Date().getFullYear() + 7;
    const period = { startDate: `${year}-03-04`, endDate: `${year}-03-08` };
    const created: number[] = [];
    try {
      created.push(
        await insert(page.request, "availability", {
          user: { id: substitute!.id },
          status: "NOT_AVAILABLE",
          ...period,
        })
      );
      const id = await insert(page.request, "availability", {
        user: { id: own!.id },
        status: "REMOTE_REACHABLE",
        substitutes: [{ id: substitute!.id }],
        ...period,
      });
      created.push(id);
      const stored = await fetchEntity<{ conflict?: boolean }>(
        page.request,
        "availability",
        id
      );
      expect(stored.conflict, "flagged by the conflict cache").toBe(true);

      await goto(page, `/availability/${id}`);
      await expect(
        page.getByText(format.t("availability.conflict.info"))
      ).toBeVisible();
      const chart = page.getByRole("img", {
        name: format.t("availability.chart.ariaLabel"),
      });
      await expect(chart).toBeVisible();
      await expect(
        chart.getByText(substitute!.displayName ?? "").first()
      ).toBeVisible();
      // The substitute's entry in the table of the absences, by its status.
      await expect(
        page
          .getByRole("cell", {
            name: format.t("availability.status.notAvailable"),
          })
          .first()
      ).toBeVisible();
    } finally {
      for (const entryId of created.reverse()) {
        await markAsDeleted(page.request, "availability", entryId);
      }
    }
  });
});

/** Matches a name beginning with the given text as a whole word. */
function startsWith(text: string): RegExp {
  return new RegExp(`^${text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}(\\s|$)`);
}
