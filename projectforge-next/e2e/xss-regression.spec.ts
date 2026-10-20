import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import {
  createBook,
  fetchEntity,
  fetchRootTaskId,
  insert,
  markAsDeleted,
  MARKER,
  uniqueSuffix,
  writeHeaders,
} from "./fixtures/seed";
import { createInvoice, removeInvoice } from "./fixtures/invoice";
import { TIMESHEET_ENTITY } from "../components/features/timesheet/timesheet.page";
import { INVOICE_PAGE } from "../components/features/invoice/invoice.page";

/**
 * Free texts are text, never markup: a stored `<img onerror>` must show up as the characters typed, and
 * must not run. Wicket rendered several of these fields as raw html (stored XSS); next renders them
 * as React text. This spec keeps it that way for the fields that were affected: a time sheet's
 * description, its task as the structure element of the list, an invoice's remarks and the change
 * history.
 *
 * Each case stores [payload] through the API, opens the page and checks three things: no dialog, the
 * payload's script didn't run (it would set `window.__pfXss`), and no element of it got into the DOM.
 * The text itself has to be visible, otherwise the check would pass for a page that shows nothing.
 */

/** The marker of the payload, the part a check finds the escaped text by. */
const PROOF = "XSS_PROOF_";

/** Markup that runs as soon as it is parsed as html, tagged with the run's suffix. */
function payload(suffix: string): string {
  return `${PROOF}${suffix}<img src=x onerror="window.__pfXss=1"><svg onload="window.__pfXss=1"></svg>`;
}

/** Fails the test on any dialog, the classic proof of an XSS (`alert(1)`). */
function failOnDialog(page: Page) {
  page.on("dialog", async (dialog) => {
    await dialog.dismiss();
    throw new Error(`Unexpected dialog: ${dialog.message()}`);
  });
}

/** The payload is shown as text and did nothing. */
async function expectInert(page: Page, suffix: string) {
  await expect(
    // The visible one: a page may keep a copy hidden (an inactive tab, a collapsed tooltip).
    page
      .getByText(`${PROOF}${suffix}<img`, { exact: false })
      .filter({ visible: true })
      .first()
  ).toBeVisible({ timeout: 30_000 });
  expect(
    await page.evaluate(
      () => (window as unknown as { __pfXss?: number }).__pfXss
    )
  ).toBeUndefined();
  await expect(page.locator('img[src="x"]')).toHaveCount(0);
  await expect(page.locator("svg[onload]")).toHaveCount(0);
}

test.describe.configure({ timeout: 120_000 });

test.describe("stored XSS: free texts stay text", { tag: "@parallel" }, () => {
  test("time sheet description and task in list and edit", async ({
    loggedInPage: page,
  }) => {
    failOnDialog(page);
    const suffix = uniqueSuffix();
    const text = payload(suffix);
    // A task of its own, with the payload as title: the list shows it as the structure element.
    const taskId = await insert(page.request, "task", {
      title: text,
      status: "O",
      parentTask: { id: await fetchRootTaskId(page.request) },
    });
    const status = await page.request.get("/rs/userStatus", {
      headers: { "X-PF-Frontend": "next" },
    });
    const { userData } = (await status.json()) as {
      userData: { userId: number };
    };
    // Far in the future, so it collides with no real booking (see timesheet-list-edit.spec.ts).
    const res = await page.request.put(`/rs/${TIMESHEET_ENTITY}/saveorupdate`, {
      headers: await writeHeaders(page.request),
      data: {
        data: {
          task: { id: taskId },
          user: { id: userData.userId },
          startTime: "2099-07-01T08:00:00.000Z",
          stopTime: "2099-07-01T09:00:00.000Z",
          description: text,
          location: text,
        },
      },
    });
    expect(res.ok(), await res.text()).toBe(true);
    const list = await page.request.post(`/rs/${TIMESHEET_ENTITY}/listPage`, {
      headers: await writeHeaders(page.request),
      data: {
        filter: { entries: [{ field: "task", value: { id: taskId } }] },
        offset: 0,
        limit: 10,
        doNotStore: true,
      },
    });
    const { resultSet } = (await list.json()) as {
      resultSet: { id: number; description?: string }[];
    };
    const timesheetId = resultSet.find((row) => row.description === text)?.id;
    expect(timesheetId).toBeDefined();
    try {
      // The task jump: a transient filter, so the stored list state stays untouched.
      await goto(page, `/timesheet?taskId=${taskId}`);
      await expectInert(page, suffix);

      await goto(page, `/timesheet/${timesheetId}`);
      await expectInert(page, suffix);
    } finally {
      if (timesheetId != null) {
        await markAsDeleted(page.request, TIMESHEET_ENTITY, timesheetId);
      }
    }
  });

  test("change history shows old and new values as text", async ({
    loggedInPage: page,
  }) => {
    failOnDialog(page);
    const suffix = uniqueSuffix();
    const book = await createBook(page.request, suffix);
    const stored = await fetchEntity<Record<string, unknown>>(
      page.request,
      "book",
      book.id
    );
    // The whole DTO, see createBookWithHistory: a partial body would clear the other fields.
    await insert(page.request, "book", {
      ...stored,
      abstractText: payload(suffix),
    });
    const { t } = await userFormat(page);

    await goto(page, `/book/${book.id}?tab=history`);
    const first = page.getByRole("listitem").first().getByRole("button");
    await expect(first).toBeVisible({ timeout: 30_000 });
    await first.click();
    await expect(
      page.getByText(t("changes"), { exact: true }).first()
    ).toBeVisible();
    await expectInert(page, suffix);
  });
});

test.describe("stored XSS: invoice remarks", { tag: "@lane-invoice" }, () => {
  test("remark in the list and both free texts in the edit form", async ({
    loggedInPage: page,
  }) => {
    failOnDialog(page);
    const suffix = uniqueSuffix();
    const subject = `${MARKER} invoice xss ${suffix}`;
    let id: number | null = null;
    try {
      id = await createInvoice(
        page,
        [{ number: 1, text: `${subject} 1`, menge: 1, einzelNetto: 100 }],
        {
          subject,
          bemerkung: payload(suffix),
          besonderheiten: payload(suffix),
        }
      );
      const format = await userFormat(page);
      // A criterion left behind by another run decides which rows the list shows (see
      // invoice-edit.spec.ts).
      await page.request
        .post(`/rs/${INVOICE_PAGE.entity}/filter/reset`, {
          headers: await writeHeaders(page.request),
        })
        .catch(() => undefined);
      await goto(page, "/invoice");
      await page.getByPlaceholder(format.t("filter.searchList")).fill(subject);
      await expect(
        page.getByRole("cell", { name: subject, exact: true })
      ).toBeVisible({ timeout: 30_000 });
      await expectInert(page, suffix);

      await goto(page, `/invoice/${id}`);
      await expect(
        page.getByLabel(label(format, "fibu.rechnung.betreff"), { exact: true })
      ).toHaveValue(subject, { timeout: 60_000 });
      // Text areas: the payload is their value, not markup of the page.
      const values = await page
        .locator("textarea")
        .evaluateAll((areas) =>
          areas.map((area) => (area as HTMLTextAreaElement).value)
        );
      expect(
        values.filter((v) => v.startsWith(`${PROOF}${suffix}`))
      ).toHaveLength(2);
      expect(
        await page.evaluate(
          () => (window as unknown as { __pfXss?: number }).__pfXss
        )
      ).toBeUndefined();
      await expect(page.locator('img[src="x"]')).toHaveCount(0);
    } finally {
      await removeInvoice(page, id);
    }
  });
});
