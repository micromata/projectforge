import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The job monitor against the live backend (`JobsMonitorRest`): the page shows exactly the jobs the backend
 * answers for the logged-in user, or says there are none. Read-only: no job is started (a re-index affects the
 * whole system) and none is cancelled.
 */
test.describe("job monitor", { tag: "@parallel" }, () => {
  test("lists the jobs of the user or says there are none", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const answer = page.waitForResponse((r) =>
      r.url().includes("/rs/jobsMonitor/jobs")
    );
    await goto(page, "/jobsMonitor");
    const body = (await (await answer).json()) as {
      variables?: { variables?: { jobs?: unknown[] } };
    };
    await expect(
      page.getByRole("heading", { name: t("jobs.monitor.title") })
    ).toBeVisible();
    // A job may start or vanish between the polls, so only the empty state is compared exactly.
    if ((body.variables?.variables?.jobs ?? []).length === 0) {
      await expect(
        page.getByText(t("jobs.monitor.noJobsAvailable"))
      ).toBeVisible();
    } else {
      await expect(page.getByTestId("job-card").first()).toBeVisible();
    }
  });

  test("cancels only per POST", async ({ loggedInPage: page }) => {
    // State changing, so no GET any more (the legacy React page used one).
    const get = await page.request.get("/rs/jobsMonitor/cancel?jobId=-1");
    expect(get.ok()).toBe(false);
  });

  test("redirects the old React page", async ({ loggedInPage: page }) => {
    await page.goto(
      "/react/jobsMonitor/dynamic?jobId=42&caller=https://example.org"
    );
    await expect(page).toHaveURL(/\/next\/jobsMonitor\?jobId=42$/);
  });
});
