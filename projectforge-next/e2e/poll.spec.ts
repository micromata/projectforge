import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import {
  insert,
  markAsDeleted,
  MARKER,
  uniqueSuffix,
  writeHeaders,
} from "./fixtures/seed";

/**
 * The polls against the live backend (see POLL_PAGE, PollEntityRest and PollAnswerRest).
 *
 * The form test only intercepts the save. The answer test creates a poll of its own with the logged-in user
 * as its only attendee (so the invitation and the mail of the end reach nobody else) and marks it deleted at
 * its end.
 */

async function loggedInUserId(page: Page): Promise<number> {
  const status = await page.request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { userData } = (await status.json()) as {
    userData: { userId: number };
  };
  return userData.userId;
}

function inDays(days: number): string {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 10);
}

test.describe("poll", { tag: "@parallel" }, () => {
  test("posts the questions of the form, a template's included", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let posted: Record<string, unknown> | undefined;
    await page.route("**/rs/poll/{insert,update}*", (route) => {
      posted = (
        route.request().postDataJSON() as { data: Record<string, unknown> }
      ).data;
      return route.abort();
    });
    await goto(page, "/poll/new");
    await page
      .getByLabel(label(format, "poll.title"), { exact: true })
      .fill(`${MARKER} poll form ${uniqueSuffix()}`);
    await page
      .getByRole("button", { name: format.t("poll.question.add") })
      .click();
    const question = page.getByTestId("poll-question").first();
    await question
      .getByLabel(`${format.t("poll.question._")} 1`, { exact: true })
      .fill("Coming?");
    await question
      .getByLabel(`${format.t("poll.option._")} 1`, { exact: true })
      .fill("Yes");
    await question
      .getByLabel(`${format.t("poll.option._")} 2`, { exact: true })
      .fill("No");
    // The questions of a built-in template are appended.
    await page
      .getByRole("combobox", { name: format.t("poll.questionTemplate._") })
      .click();
    await page.getByRole("option").first().click();
    await expect(page.getByTestId("poll-question").nth(1)).toBeVisible();
    const count = await page.getByTestId("poll-question").count();
    // Moved down: the first question is the second one now.
    await question
      .getByRole("button", { name: format.t("poll.question.moveDown") })
      .click();

    await page
      .getByRole("button", { name: format.t("create"), exact: true })
      .or(page.getByRole("button", { name: format.t("save"), exact: true }))
      .first()
      .click();
    await expect.poll(() => posted).toBeDefined();
    const questions = posted!.questions as {
      text: string;
      type: string;
      options: { text: string }[];
    }[];
    expect(questions).toHaveLength(count);
    expect(questions[1]).toMatchObject({
      text: "Coming?",
      type: "SINGLE_CHOICE",
      options: [{ text: "Yes" }, { text: "No" }],
    });
  });

  test("answers a poll, shows the results and finishes it", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const userId = await loggedInUserId(page);
    const title = `${MARKER} poll ${uniqueSuffix()}`;
    const id = await insert(page.request, "poll", {
      title,
      deadline: inDays(7),
      owner: { id: userId },
      attendeeUsers: [{ id: userId }],
      questions: [
        {
          text: "Coming?",
          type: "SINGLE_CHOICE",
          options: [{ text: "Yes" }, { text: "No" }],
        },
        {
          text: "Food?",
          type: "MULTIPLE_CHOICE",
          options: [{ text: "Meat" }, { text: "Fish" }, { text: "Veggie" }],
        },
        { text: "Remarks", type: "TEXT", options: [] },
      ],
    });
    try {
      // Found by the search of the list (title, description and location are indexed).
      await page.request.post("/rs/poll/filter/reset", {
        headers: await writeHeaders(page.request),
      });
      await goto(page, "/poll");
      await page.getByPlaceholder(format.t("filter.searchList")).fill(title);
      await expect(
        page.getByRole("cell", { name: title, exact: true })
      ).toBeVisible({ timeout: 30_000 });

      await goto(page, `/poll/${id}/answer`);
      const questions = page.getByTestId("poll-answer-question");
      await expect(questions).toHaveCount(3, { timeout: 30_000 });
      await questions.nth(0).getByRole("radio", { name: "No" }).click();
      // Several options of a multiple choice question:
      await questions.nth(1).getByRole("checkbox", { name: "Meat" }).click();
      await questions.nth(1).getByRole("checkbox", { name: "Veggie" }).click();
      await questions.nth(2).getByRole("textbox").fill("See you");
      await page
        .getByRole("button", { name: format.t("poll.answer.save") })
        .click();
      await expect(page.getByText(format.t("poll.answer.saved"))).toBeVisible();

      // Stored: shown again after a reload.
      await page.reload();
      await expect(
        questions.nth(0).getByRole("radio", { name: "No" })
      ).toBeChecked({ timeout: 30_000 });
      await expect(
        questions.nth(1).getByRole("checkbox", { name: "Meat" })
      ).toBeChecked();
      await expect(
        questions.nth(1).getByRole("checkbox", { name: "Fish" })
      ).not.toBeChecked();
      await expect(questions.nth(2).getByRole("textbox")).toHaveValue(
        "See you"
      );

      await goto(page, `/poll/${id}?tab=results`);
      await expect(page.getByTestId("poll-results-summary")).toHaveText(
        format.t("poll.results.answered", { arg0: 1, arg1: 1 }),
        { timeout: 30_000 }
      );

      await page
        .getByRole("button", { name: format.t("poll.finish._") })
        .click();
      await page
        .getByRole("alertdialog")
        .getByRole("button", { name: format.t("poll.finish._") })
        .click();
      await expect(page.getByText(format.t("poll.finish.done"))).toBeVisible();

      await goto(page, `/poll/${id}/answer`);
      await expect(page.getByText(format.t("poll.answer.notOpen"))).toBeVisible(
        {
          timeout: 30_000,
        }
      );
      await expect(
        questions.nth(0).getByRole("radio", { name: "Yes" })
      ).toBeDisabled();
      await expect(
        page.getByRole("button", { name: format.t("poll.answer.save") })
      ).toHaveCount(0);
    } finally {
      await markAsDeleted(page.request, "poll", id);
    }
  });
});
